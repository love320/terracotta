/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.dmi;

import com.tc.util.Assert;

public class DmiClassSpec {

  private final String classLoaderDesc;
  private final String className;
  private final String spec;

  public DmiClassSpec(final String classLoaderDesc, final String className) {
    Assert.pre(classLoaderDesc != null);
    Assert.pre(className != null);
    this.classLoaderDesc = classLoaderDesc;
    this.className = className;
    this.spec = classLoaderDesc + "-" + className;
  }

  public String getClassLoaderDesc() {
    return classLoaderDesc;
  }

  public String getClassName() {
    return className;
  }

  public int hashCode() {
    return spec.hashCode();
  }

  public boolean equals(Object obj) {
    if (!(obj instanceof DmiClassSpec)) return false;
    DmiClassSpec that = (DmiClassSpec) obj;
    return this.spec.equals(that.spec);
  }
}
