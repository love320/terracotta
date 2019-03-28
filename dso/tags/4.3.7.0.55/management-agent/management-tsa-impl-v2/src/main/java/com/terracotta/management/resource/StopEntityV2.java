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
package com.terracotta.management.resource;

import java.io.Serializable;

public class StopEntityV2 implements Serializable {
  private boolean forceStop;
  private boolean stopIfActive;
  private boolean stopIfPassive;
  private boolean restart;
  private boolean restartInSafeMode;

  public boolean isForceStop() {
    return forceStop;
  }

  public void setForceStop(boolean forceStop) {
    this.forceStop = forceStop;
  }


  public boolean isStopIfActive() {
    return stopIfActive;
  }

  public void setStopIfActive(boolean stopIfActive) {
    this.stopIfActive = stopIfActive;
  }

  public boolean isStopIfPassive() {
    return stopIfPassive;
  }

  public void setStopIfPassive(boolean stopIfPassive) {
    this.stopIfPassive = stopIfPassive;
  }

  public boolean isRestart() {
    return restart;
  }

  public void setRestart(boolean restart) {
    this.restart = restart;
  }

  public boolean isRestartInSafeMode() {
    return restartInSafeMode;
  }

  public void setRestartInSafeMode(boolean restartInSafeMode) {
    this.restartInSafeMode = restartInSafeMode;
  }
}
