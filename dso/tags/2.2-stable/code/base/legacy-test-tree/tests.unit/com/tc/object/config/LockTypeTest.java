/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.config;

import junit.framework.TestCase;

public class LockTypeTest extends TestCase {

  public void testLockTypeByName() {
    assertSame(ConfigLockLevel.WRITE, ConfigLockLevel.lockLevelByName(ConfigLockLevel.WRITE_NAME));
    assertSame(ConfigLockLevel.READ, ConfigLockLevel.lockLevelByName(ConfigLockLevel.READ_NAME));
  }

}
