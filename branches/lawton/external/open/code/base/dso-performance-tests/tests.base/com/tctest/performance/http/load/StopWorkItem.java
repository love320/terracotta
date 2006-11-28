/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest.performance.http.load;

public class StopWorkItem implements WorkItem {

  public void execute(StatsCollector c) {
    throw new RuntimeException("Wow! Error in the Code!");
  }

  public boolean stop() {
    return true;
  }

  public boolean expired(long currenttime) {
    return false;
  }

  public void done() {
    //
  }
}
