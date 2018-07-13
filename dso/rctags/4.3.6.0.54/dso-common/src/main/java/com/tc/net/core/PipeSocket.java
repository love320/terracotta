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
package com.tc.net.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.Pipe;
import java.nio.channels.Pipe.SinkChannel;
import java.nio.channels.Pipe.SourceChannel;

/**
 * @author Ludovic Orban
 */
public abstract class PipeSocket extends Socket {

  private final Pipe       inputPipe;
  private final Pipe       outputPipe;
  private final Socket     socket;
  private final PipeSocketOutputStream outputStream;
  private final InputStream inputStream;

  private volatile boolean readClosed = false;
  private volatile boolean writeClosed = false;
  private volatile Throwable closeReadReason = null;
  private volatile Throwable closeWriteReason = null;

  public PipeSocket(Socket socket) throws IOException {
    this.socket = socket;
    this.inputPipe = Pipe.open();
    this.outputPipe = Pipe.open();
    this.outputPipe.source().configureBlocking(false);
    this.inputStream = Channels.newInputStream(inputPipe.source());
    this.outputStream = new PipeSocketOutputStream(Channels.newOutputStream(outputPipe.sink()));
  }

  public SourceChannel getOutputPipeSourceChannel() {
    return outputPipe.source();
  }

  public SinkChannel getInputPipeSinkChannel() {
    return inputPipe.sink();
  }

  @Override
  public InputStream getInputStream() throws IOException {
    if (readClosed) {
      throw new IOException("Read side is closed", closeReadReason);
    }
    return inputStream;
  }

  @Override
  public OutputStream getOutputStream() throws IOException {
    if (writeClosed) {
      throw new IOException("Write side is closed", closeWriteReason);
    }
    return outputStream;
  }

  @Override
  public SocketAddress getRemoteSocketAddress() {
    if (isClosed()) { return null; }
    return socket.getRemoteSocketAddress();
  }

  @Override
  public SocketAddress getLocalSocketAddress() {
    if (isClosed()) { return null; }
    return socket.getLocalSocketAddress();
  }

  @Override
  public InetAddress getLocalAddress() {
    if (isClosed()) { return null; }
    return socket.getLocalAddress();
  }

  @Override
  public boolean isClosed() {
    return readClosed && writeClosed;
  }

  public boolean isReadClosed() {
    return readClosed;
  }

  public boolean isWriteClosed() {
    return writeClosed;
  }

  private boolean isWritePipeBroken() {
    return closeWriteReason != null;
  }

  @Override
  public synchronized void close() throws IOException {
    if (isClosed()) return;
    super.close();
    closeRead(null);
    closeWrite(null);
    socket.close();
  }

  public abstract void onWrite(int len);

  public abstract void onFlush();

  public void closeRead(IOException ioe) throws IOException {
    if (this.readClosed) return;
    this.closeReadReason = ioe;
    this.readClosed = true;
    inputStream.close();
    inputPipe.sink().close();
    inputPipe.source().close();
  }

  public void closeWrite(IOException ioe) throws IOException {
    if (this.writeClosed) return;
    this.closeWriteReason = ioe;
    outputStream.close();
    outputPipe.sink().close();
    outputPipe.source().close();
  }

  private final class PipeSocketOutputStream extends OutputStream {

    private final OutputStream delegate;

    PipeSocketOutputStream(OutputStream delegate) {
      this.delegate = delegate;
    }

    @Override
    public void write(int b) throws IOException {
      if (writeClosed) {
        throw new IOException("Write side is closed", closeWriteReason);
      }
      delegate.write(b);
      onWrite(1);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
      if (writeClosed) {
        throw new IOException("Write side is closed", closeWriteReason);
      }
      delegate.write(b, off, len);
      onWrite(len);
    }

    @Override
    public void flush() throws IOException {
      if (writeClosed) {
        throw new IOException("Write side is closed", closeWriteReason);
      }
      delegate.flush();
      onFlush();
    }

    @Override
    public void close() throws IOException {
      if (writeClosed) return;
      if (!isWritePipeBroken()) {
        delegate.flush();
        onFlush();
      }
      delegate.close();
      writeClosed = true;
    }
  }
}
