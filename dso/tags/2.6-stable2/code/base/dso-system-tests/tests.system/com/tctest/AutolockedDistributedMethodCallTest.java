/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

public class AutolockedDistributedMethodCallTest extends TransparentTestBase implements TestConfigurator {

  private static final int NODE_COUNT           = 3;
  private static final int LOOP_ITERATION_COUNT = 1;

  public AutolockedDistributedMethodCallTest() {
    // disableAllUntil("2007-11-15");
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT).setIntensity(LOOP_ITERATION_COUNT);
    t.initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return AutolockedDistributedMethodCallTestApp.class;
  }

}
