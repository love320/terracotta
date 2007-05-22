/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.process;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A simple thread that copies one stream to another. Useful for copying a process's output/error streams to this
 * process's output/error streams.
 */
public class StreamCopier extends Thread {

  protected final InputStream  in;
  protected final OutputStream out;
  private final String         identifier;

  public StreamCopier(InputStream stream, OutputStream out) {
    this(stream, out, null);
  }

  public StreamCopier(InputStream stream, OutputStream out, String identifier) {
    if ((stream == null) || (out == null)) { throw new AssertionError("null streams not allowed"); }

    this.in = stream;
    this.out = out;
    setName("Stream Copier");
    setDaemon(true);
    this.identifier = identifier;
  }

  public void run() {
    byte[] buf = new byte[4096];

    try {
      int read;
      while ((read = in.read(buf)) >= 0) {
        if (identifier != null) {
          out.write(identifier.getBytes());
        }
        out.write(buf, 0, read);
      }
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }
  }

}