/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.io;

import java.io.FileNotFoundException;

public interface TCRandomFileAccess {
  public TCFileChannel getChannel(TCFile tcFile, String mode) throws FileNotFoundException;
}
