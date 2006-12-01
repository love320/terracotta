/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.common.XTree;
import com.tc.admin.common.XRootNode;

public class NavTree extends XTree {
  public NavTree() {
    super();
  }

  public XRootNode getRootNode() {
    return (XRootNode)getModel().getRoot();
  }
}
