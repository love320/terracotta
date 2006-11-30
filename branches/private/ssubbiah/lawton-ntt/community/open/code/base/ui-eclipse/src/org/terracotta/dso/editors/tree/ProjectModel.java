/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package org.terracotta.dso.editors.tree;

import org.eclipse.core.resources.IProject;

import com.tc.admin.common.XRootNode;
import com.tc.admin.common.XTreeModel;

public class ProjectModel extends XTreeModel {
  public ProjectModel(IProject project) {
    this(new ProjectRoot(project));
  }
  
  public ProjectModel(XRootNode root) {
    super(root);
  }
}

