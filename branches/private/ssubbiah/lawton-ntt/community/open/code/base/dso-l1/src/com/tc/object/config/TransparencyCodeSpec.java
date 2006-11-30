/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object.config;

import com.tc.object.bytecode.ByteCodeUtil;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

public class TransparencyCodeSpec {
  private final static Set MONITOR_INSTRUMENTATION_REQ_LOGICAL_CLASS = new HashSet();
  
  private boolean arrayOperatorInstrumentationReq;
  private boolean arraycopyInstrumentationReq;
  private boolean fieldInstrumentationReq;
  private boolean waitNotifyInstrumentationReq;
  private boolean monitorInstrumentationReq;
  
  static {
    MONITOR_INSTRUMENTATION_REQ_LOGICAL_CLASS.add(Hashtable.class.getName());
  }

  public static TransparencyCodeSpec getDefaultPhysicalCodeSpec() {
    TransparencyCodeSpec defaultPhysicalCodeSpec = new TransparencyCodeSpec();
    defaultPhysicalCodeSpec.setArrayOperatorInstrumentationReq(true);
    defaultPhysicalCodeSpec.setArraycopyInstrumentationReq(true);
    defaultPhysicalCodeSpec.setFieldInstrumentationReq(true);
    defaultPhysicalCodeSpec.setWaitNotifyInstrumentationReq(true);
    defaultPhysicalCodeSpec.setMonitorInstrumentationReq(true);
    return defaultPhysicalCodeSpec;
  }

  public static TransparencyCodeSpec getDefaultLogicalCodeSpec() {
    TransparencyCodeSpec defaultSpec = new TransparencyCodeSpec();
    return defaultSpec;
  }

  public static TransparencyCodeSpec getDefaultCodeSpec(String className, boolean isLogical, boolean isAutolock) {
    if (isLogical) {
      TransparencyCodeSpec codeSpec = getDefaultLogicalCodeSpec();
      if (MONITOR_INSTRUMENTATION_REQ_LOGICAL_CLASS.contains(className)) {
        codeSpec.setMonitorInstrumentationReq(isAutolock);
      }
      return codeSpec;
    }
    return getDefaultPhysicalCodeSpec();
  }

  public TransparencyCodeSpec() {
    super();
  }

  public boolean isArraycopyInstrumentationReq(String className, String methodName) {
    return arraycopyInstrumentationReq && "java/lang/System".equals(className) && "arraycopy".equals(methodName);
  }

  public void setArraycopyInstrumentationReq(boolean arraycopyInstrumentationReq) {
    this.arraycopyInstrumentationReq = arraycopyInstrumentationReq;
  }

  public boolean isArrayOperatorInstrumentationReq() {
    return arrayOperatorInstrumentationReq;
  }

  public void setArrayOperatorInstrumentationReq(boolean arrayOperatorInstrumentationReq) {
    this.arrayOperatorInstrumentationReq = arrayOperatorInstrumentationReq;
  }

  public boolean isFieldInstrumentationReq(String fieldName) {
    // For jdk compiler, the <init> method of an anonymous inner class contains
    // code to set the synthetic fields, followed by the super() call. This creates
    // problem if we try to use TC setter method for the synthetic fields before
    // the super() method. So, before the super() call, we will not instrument
    // to use the TC setter method.
    return fieldInstrumentationReq && !ByteCodeUtil.isTCSynthetic(fieldName);
  }

  public void setFieldInstrumentationReq(boolean fieldInstrumentationReq) {
    this.fieldInstrumentationReq = fieldInstrumentationReq;
  }

  public boolean isWaitNotifyInstrumentationReq() {
    return waitNotifyInstrumentationReq;
  }

  public void setWaitNotifyInstrumentationReq(boolean waitNotifyInstrumentationReq) {
    this.waitNotifyInstrumentationReq = waitNotifyInstrumentationReq;
  }

  public boolean isMonitorInstrumentationReq() {
    return monitorInstrumentationReq;
  }

  public void setMonitorInstrumentationReq(boolean monitorInstrumentationReq) {
    this.monitorInstrumentationReq = monitorInstrumentationReq;
  }

}
