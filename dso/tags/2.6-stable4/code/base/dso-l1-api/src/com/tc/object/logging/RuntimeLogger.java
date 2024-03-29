/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.logging;

import com.tc.object.TCObject;
import com.tc.object.tx.TimerSpec;

/**
 * Logging interface for various DSO runtime events
 */
public interface RuntimeLogger {

  // /////////////////////////////
  // logging options
  // /////////////////////////////

  void setLockDebug(boolean lockDebug);
  boolean getLockDebug();

  void setFieldChangeDebug(boolean fieldChangeDebug);
  boolean getFieldChangeDebug();

  void setArrayChangeDebug(boolean arrayChangeDebug);
  boolean getArrayChangeDebug();

  void setNewManagedObjectDebug(boolean newObjectDebug);
  boolean getNewManagedObjectDebug();

  void setDistributedMethodDebug(boolean distributedMethodDebug);
  boolean getDistributedMethodDebug();

  void setWaitNotifyDebug(boolean waitNotifyDebug);
  boolean getWaitNotifyDebug();

  void setNonPortableDump(boolean nonPortableDump);
  boolean getNonPortableDump();

  void setFullStack(boolean fullStack);
  boolean getFullStack();
  
  void setCaller(boolean caller);
  boolean getCaller();
  
  void setAutoLockDetails(boolean autoLockDetails);
  boolean getAutoLockDetails();
  
  // /////////////////////////////
  // log methods
  // /////////////////////////////

  void lockAcquired(String lockName, int level, Object instance, TCObject tcobj);

  void literalValueChanged(TCObject source, Object newValue);

  void fieldChanged(TCObject source, String classname, String fieldname, Object newValue, int index);

  void arrayChanged(TCObject source, int startPos, Object array);

  void newManagedObject(TCObject object);

  void objectNotify(boolean all, Object obj, TCObject tcObject);

  void objectWait(TimerSpec call, Object obj, TCObject tcObject);

  void distributedMethodCall(String receiverClassName, String methodName, String params);

  void distributedMethodCallError(String obj, String methodName, String params, Throwable error);

}
