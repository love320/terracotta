/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest;

import com.tc.util.concurrent.ThreadUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * writer for the FastReadSlowWriteTest
 */
public class TestWriter {
  public final static int         WRITE_COUNT  = 100;
  public final static int         WRITE_DELAY  = 10;

  private Map    stuff = new HashMap();
  private Random r     = new Random();

  public void write() {
    int count = 0;
    while (count++ < WRITE_COUNT) {
      doAWrite();
      ThreadUtil.reallySleep(WRITE_DELAY);
    }
  }

  public void doAWrite() {
    synchronized (stuff) {
      stuff.put(new Integer(stuff.size() + 1), "" + r.nextLong());
    }
  }

}
