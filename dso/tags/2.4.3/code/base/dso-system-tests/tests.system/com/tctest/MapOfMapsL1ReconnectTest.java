/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

public class MapOfMapsL1ReconnectTest extends TransparentTestBase implements TestConfigurator {
  private static final int NODE_COUNT    = 2;
  private static final int THREADS_COUNT = 2;

  protected Class getApplicationClass() {
    return MapOfMapsTestApp.class;
  }
  
  protected boolean enableL1Reconnect() {
    return true;
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT).setApplicationInstancePerClientCount(THREADS_COUNT);
    t.initializeTestRunner();
  }

}
