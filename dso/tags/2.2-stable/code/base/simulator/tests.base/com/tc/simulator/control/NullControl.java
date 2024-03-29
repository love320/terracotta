/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.simulator.control;


public class NullControl implements Control {

  public void waitForStart(long timeout) {
    return;
  }

  public void notifyComplete() {
    return;
  }

  public boolean waitForAllComplete(long timeout) {
    return true;
  }

}
