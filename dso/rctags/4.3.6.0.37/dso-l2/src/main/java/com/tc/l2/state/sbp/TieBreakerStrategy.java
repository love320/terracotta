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
package com.tc.l2.state.sbp;

import com.tc.config.schema.setup.FailOverAction;
import com.tc.util.State;

public interface TieBreakerStrategy {
  
  enum Result {
    
    /* The result if the requesting node has won the tie breaking attempt */
    WON,
    
    /**
     *  The result if the requesting node has NOT won the the tie breaking
     *  attempt AND this tie-breaker is aware that another node 
     *  has already WON */
    LOST,

    /**
     *  The result if the tie-breaker is not able to break the tie OR 
     *  if the requesting node has NOT won the the tie breaking
     *  attempt and this tie-breaker is not aware that another node 
     *  has already WON */
    DUBIOUS
  }
  
  Result breakTie(State state);

  /**
   * This method can be used as a hook for external intervention when
   * the tie-breaker is not able to break a tie on its own.
   * 
   * @param failOverAction
   */
  void performExternalTieBreaking(FailOverAction failOverAction);
  
  boolean isWaitingForExternalTieBreaking();
}
