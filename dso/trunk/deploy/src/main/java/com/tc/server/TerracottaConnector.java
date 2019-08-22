/*
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.server;

import com.tc.util.MultiIOExceptionHandler;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.LocalConnector;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * A Jetty connector that is handed sockets from the DSO listen port once they are identified as HTTP requests
 */
public class TerracottaConnector extends LocalConnector {

  private static final Logger LOGGER = LoggerFactory.getLogger(TerracottaConnector.class);
  static final int DEFAULT_IDLE_TIMEOUT_IN_MS = 30000;
  private final ExecutorService executorService;
  private final Consumer<Socket> reclaimer;

  public TerracottaConnector(Server server, HttpConnectionFactory httpConnectionFactory, Consumer<Socket> reclaimer) {
    this(server, httpConnectionFactory, new ThreadPoolExecutor(2, 64, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), new ThreadFactory() {
      final AtomicInteger counter = new AtomicInteger();
      @Override
      public Thread newThread(Runnable r) {
        return new Thread(r, "Jetty Connector - " + counter.incrementAndGet());
      }
    }), DEFAULT_IDLE_TIMEOUT_IN_MS, reclaimer);
  }

  TerracottaConnector(Server server, HttpConnectionFactory httpConnectionFactory, int idleTimeoutInMs, Consumer<Socket> reclaimer) {
    this(server, httpConnectionFactory, new ThreadPoolExecutor(2, 64, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), new ThreadFactory() {
      final AtomicInteger counter = new AtomicInteger();
      @Override
      public Thread newThread(Runnable r) {
        return new Thread(r, "Jetty Connector - " + counter.incrementAndGet());
      }
    }), idleTimeoutInMs, reclaimer);
  }

  TerracottaConnector(Server server, HttpConnectionFactory httpConnectionFactory, ExecutorService executor, int idleTimeoutInMs, Consumer<Socket> reclaimer) {
    super(server, httpConnectionFactory);
    this.executorService = executor;
    setIdleTimeout(idleTimeoutInMs);
    this.reclaimer = reclaimer;
  }

  @Override
  public Future<Void> shutdown() {
    executorService.shutdownNow();
    return super.shutdown();
  }

  public Future<?> handleSocketFromDSO(Socket socket, byte[] data) {
    Consumer<Socket> reclaimer = this.reclaimer;
    if (reclaimer != null) {
      reclaimer.accept(socket);
    }
    LocalEndPoint endPoint = this.connect();
    Future<?> reader = spawnReader(socket, data, endPoint);
    return spawnWriter(socket, endPoint, reader);
  }

  Future<?> spawnReader(Socket socket, byte[] data, LocalEndPoint endPoint) {
    try {
      return executorService.submit(() -> {
        try {
          endPoint.addInput(ByteBuffer.wrap(data));

          try (InputStream inputStream = socket.getInputStream()) {
            while (true) {
              byte[] buffer = new byte[128];
              int read = inputStream.read(buffer);
              if (read == -1) {
                break;
              }
              endPoint.addInput(ByteBuffer.wrap(buffer, 0, read));
            }
          }
        } catch (Exception e) {
          if (!(IOException.class.isAssignableFrom(e.getClass()))) {
            LOGGER.error("Error processing an HTTP request (reader side)", e);
          }
        }
      });
    } catch (RuntimeException re) {
      // the thread pool is too busy, abandon this request
      MultiIOExceptionHandler m = new MultiIOExceptionHandler();
      m.doSafely(endPoint::close);
      m.doSafely(socket::close);
      m.addAsSuppressedTo(re);
      throw re;
    }
  }

  Future<?> spawnWriter(Socket socket, LocalEndPoint endPoint, Future<?> reader) {
    try {
      return executorService.submit(() -> {
        try {
          ByteBuffer byteBuffer = endPoint.waitForOutput(getIdleTimeout(), TimeUnit.MILLISECONDS);
          if (byteBuffer != null && byteBuffer.remaining() > 0) {
            try (WritableByteChannel channel = Channels.newChannel(socket.getOutputStream())) {
              channel.write(byteBuffer);
            }
          }
        } catch (Exception e) {
          if (!(IOException.class.isAssignableFrom(e.getClass()))) {
            LOGGER.error("Error processing an HTTP request (writer side)", e);
          }
        } finally {
          MultiIOExceptionHandler m = new MultiIOExceptionHandler();
          m.doSafely(() -> reader.cancel(true));
          m.doSafely(endPoint::close);
          m.doSafely(socket::close);
        }
      });
    } catch (RuntimeException re) {
      // the thread pool is too busy, abandon this request
      MultiIOExceptionHandler m = new MultiIOExceptionHandler();
      m.doSafely(() -> reader.cancel(true));
      m.doSafely(endPoint::close);
      m.doSafely(socket::close);
      m.addAsSuppressedTo(re);
      throw re;
    }
  }

}
