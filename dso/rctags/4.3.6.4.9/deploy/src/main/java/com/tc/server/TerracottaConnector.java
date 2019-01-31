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

import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.LocalConnector;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A Jetty connector that is handed sockets from the DSO listen port once they are identified as HTTP requests
 */
public class TerracottaConnector extends LocalConnector {

  private static final Logger LOGGER = LoggerFactory.getLogger(TerracottaConnector.class);
  private static final int IDLE_TIMEOUT_IN_MS = 30000;
  private final ExecutorService executorService;

  public TerracottaConnector(Server server, HttpConnectionFactory httpConnectionFactory) {
    super(server, httpConnectionFactory);
    setIdleTimeout(IDLE_TIMEOUT_IN_MS);
    ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 64, 30, TimeUnit.SECONDS, new SynchronousQueue<>(), new ThreadFactory() {
      final AtomicInteger counter = new AtomicInteger();
      @Override
      public Thread newThread(Runnable r) {
        return new Thread(r, "Jetty Connector - " + counter.incrementAndGet());
      }
    });
    executorService = executor;
  }

  @Override
  public Future<Void> shutdown() {
    executorService.shutdownNow();
    return super.shutdown();
  }

  public void handleSocketFromDSO(Socket socket, byte[] data) throws IOException {
    LocalConnector.LocalEndPoint endPoint = this.connect();
    Future<Object> reader = executorService.submit(new Callable<Object>() {
      @Override
      public Object call() throws Exception {
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

        return null;
      }
    });

    executorService.submit(new Callable<Object>() {
      @Override
      public Object call() throws Exception {
        try {
          ByteBuffer byteBuffer = endPoint.waitForOutput(IDLE_TIMEOUT_IN_MS, TimeUnit.MILLISECONDS);
          reader.cancel(true);
          if (byteBuffer.remaining() > 0) {
            try (WritableByteChannel channel = Channels.newChannel(socket.getOutputStream())) {
              channel.write(byteBuffer);
            }
          }
        } catch (Exception e) {
          LOGGER.error("Error processing an HTTP request", e);
        } finally {
          endPoint.close();
          socket.close();
        }

        return null;
      }
    });
  }

}
