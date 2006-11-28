/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object.bytecode.hook.impl;

import com.tc.object.TCObject;
import com.tc.object.bytecode.Manageable;

public class Util {

  private Util() {
    //
  }

  public static void exit() {
    exit(null);
  }

  public static void exit(Throwable t) {
    if (t != null) {
      t.printStackTrace(System.err);
      System.err.flush();
    }

    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      // ignore
    }

    System.exit(1);
  }


  /**
   * This method resolves all the references in an object before clone is called so that clone can clone all the
   * references. This is called from the instrumented code.
   */
  public static void resolveAllReferencesBeforeClone(Object toBeCloned) {
    if (toBeCloned instanceof Manageable) {
      TCObject tcObject = ((Manageable) toBeCloned).__tc_managed();
      if (tcObject != null) {
        tcObject.resolveAllReferences();
      }
    }
  }

  /**
   * This method unsets the TCObject reference in the cloned object if the clone() method had done a shallow copy. This
   * is called from the instrumented code.
   */
  public static Object fixTCObjectReferenceOfClonedObject(Object original, Object clone) {
    if (clone instanceof Manageable && original instanceof Manageable) {
      Manageable mClone = (Manageable) clone;
      Manageable mOriginal = (Manageable) original;
      if (mClone.__tc_managed() != null && clone != original && mClone.__tc_managed() == mOriginal.__tc_managed()) {
        // A shallow copy is returned. We dont want the clone to have the same TCObject
        mClone.__tc_managed(null);
      }
    }
    return clone;
  }

}
