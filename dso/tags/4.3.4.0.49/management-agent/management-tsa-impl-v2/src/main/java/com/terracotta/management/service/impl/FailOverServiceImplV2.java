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
package com.terracotta.management.service.impl;

import com.tc.config.schema.setup.FailOverAction;
import com.terracotta.management.service.FailOverServiceV2;
import com.terracotta.management.service.impl.util.LocalManagementSource;

public class FailOverServiceImplV2 implements FailOverServiceV2 {

  private final LocalManagementSource localManagementSource;


  public FailOverServiceImplV2(final LocalManagementSource localManagementSource) {
    this.localManagementSource = localManagementSource;
  }

  @Override
  public void promote() {
    localManagementSource.performFailOverAction(FailOverAction.PROMOTE);
  }

  @Override
  public void restart() {
    localManagementSource.performFailOverAction(FailOverAction.RESTART);
  }

  @Override
  public void failFast() {
    localManagementSource.performFailOverAction(FailOverAction.FAILFAST);
  }

}
