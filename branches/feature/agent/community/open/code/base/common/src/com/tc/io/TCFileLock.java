/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.io;

import java.io.IOException;

public interface TCFileLock {
  
  public void release() throws IOException;
}
