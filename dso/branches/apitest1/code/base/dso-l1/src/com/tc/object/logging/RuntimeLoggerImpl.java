/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.logging;

import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.object.TCObject;
import com.tc.object.bytecode.ByteCodeUtil;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.loaders.NamedClassLoader;
import com.tc.object.lockmanager.api.LockLevel;
import com.tc.object.tx.TimerSpec;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.statistics.util.NullStatsRecorder;
import com.tc.statistics.util.StatsPrinter;
import com.tc.statistics.util.StatsRecorder;

import java.text.MessageFormat;

public class RuntimeLoggerImpl implements RuntimeLogger {
  private final TCLogger logger;

  private boolean        lockDebug;
  private boolean        fieldChangeDebug;
  private boolean        arrayChangeDebug;
  private boolean        newManagedObjectDebug;
  private boolean        distributedMethodDebug;
  private boolean        nonPortableDump;
  private boolean        waitNotifyDebug;

  private boolean        fullStack;
  private boolean        autoLockDetails;

  private boolean        flushDebug;
  private StatsRecorder  flushStatsRecorder;

  private boolean        faultDebug;
  private StatsRecorder  faultStatsRecorder;

  private boolean namedLoaderDebug;

  public RuntimeLoggerImpl(DSOClientConfigHelper configHelper) {
    this.logger = CustomerLogging.getDSORuntimeLogger();

    // runtime logging items
    this.lockDebug = configHelper.runtimeLoggingOptions().logLockDebug().getBoolean();
    this.fieldChangeDebug = configHelper.runtimeLoggingOptions().logFieldChangeDebug().getBoolean();
    this.arrayChangeDebug = fieldChangeDebug;
    this.newManagedObjectDebug = configHelper.runtimeLoggingOptions().logNewObjectDebug().getBoolean();
    this.distributedMethodDebug = configHelper.runtimeLoggingOptions().logDistributedMethodDebug().getBoolean();
    this.nonPortableDump = configHelper.runtimeLoggingOptions().logNonPortableDump().getBoolean();
    this.waitNotifyDebug = configHelper.runtimeLoggingOptions().logWaitNotifyDebug().getBoolean();
    this.namedLoaderDebug = configHelper.runtimeLoggingOptions().logNamedLoaderDebug().getBoolean();

    // runtime logging options
    this.fullStack = configHelper.runtimeOutputOptions().doFullStack().getBoolean();
    this.autoLockDetails = configHelper.runtimeOutputOptions().doAutoLockDetails().getBoolean();

    setFlushDebug(TCPropertiesImpl.getProperties()
        .getBoolean(TCPropertiesConsts.L1_OBJECTMANAGER_FLUSH_LOGGING_ENABLED));
    setFaultDebug(TCPropertiesImpl.getProperties()
        .getBoolean(TCPropertiesConsts.L1_OBJECTMANAGER_FAULT_LOGGING_ENABLED));

  }

  public void setLockDebug(boolean lockDebug) {
    this.lockDebug = lockDebug;
  }

  public boolean getLockDebug() {
    return this.lockDebug;
  }

  public void setFieldChangeDebug(boolean fieldChangeDebug) {
    this.fieldChangeDebug = fieldChangeDebug;
  }

  public boolean getFieldChangeDebug() {
    return this.fieldChangeDebug;
  }

  public void setArrayChangeDebug(boolean arrayChangeDebug) {
    this.arrayChangeDebug = arrayChangeDebug;
  }

  public boolean getArrayChangeDebug() {
    return this.arrayChangeDebug;
  }

  public void setNewManagedObjectDebug(boolean newManagedObjectDebug) {
    this.newManagedObjectDebug = newManagedObjectDebug;
  }

  public boolean getNewManagedObjectDebug() {
    return this.newManagedObjectDebug;
  }

  public void setWaitNotifyDebug(boolean waitNotifyDebug) {
    this.waitNotifyDebug = waitNotifyDebug;
  }

  public boolean getWaitNotifyDebug() {
    return this.waitNotifyDebug;
  }

  public void setDistributedMethodDebug(boolean distributedMethodDebug) {
    this.distributedMethodDebug = distributedMethodDebug;
  }

  public boolean getDistributedMethodDebug() {
    return this.distributedMethodDebug;
  }

  public void setNonPortableDump(boolean nonPortableDump) {
    this.nonPortableDump = nonPortableDump;
  }

  public boolean getNonPortableDump() {
    return this.nonPortableDump;
  }

  public void setFullStack(boolean fullStack) {
    this.fullStack = fullStack;
  }

  public boolean getFullStack() {
    return this.fullStack;
  }

  public void setCaller(boolean caller) {
    // deprecated (see CDV-731, CDV-815)
  }

  public boolean getCaller() {
    // deprecated (see CDV-731, CDV-815)
    return false;
  }

  public void setAutoLockDetails(boolean autoLockDetails) {
    this.autoLockDetails = autoLockDetails;
  }

  public boolean getAutoLockDetails() {
    return this.autoLockDetails;
  }

  public void setFlushDebug(boolean flushDebug) {
    this.flushDebug = flushDebug;
    if (flushStatsRecorder != null) {
      flushStatsRecorder.finish();
    }
    if (flushDebug) {
      flushStatsRecorder = new StatsPrinter(new MessageFormat("ManagedObjects flushed in the Last {0} ms"),
                                            new MessageFormat(" {0} instances"), true);
    } else {
      flushStatsRecorder = new NullStatsRecorder();
    }
  }

  public boolean getFlushDebug() {
    return this.flushDebug;
  }

  public void updateFlushStats(String type) {
    flushStatsRecorder.updateStats(type, StatsRecorder.SINGLE_INCR);
  }

  public void setFaultDebug(boolean faultDebug) {
    this.faultDebug = faultDebug;
    if (faultStatsRecorder != null) {
      faultStatsRecorder.finish();
    }
    if (faultDebug) {
      faultStatsRecorder = new StatsPrinter(new MessageFormat("ManagedObjects faulted in the Last {0} ms"),
                                            new MessageFormat(" {0} instances"), true);
    } else {
      faultStatsRecorder = new NullStatsRecorder();
    }
  }

  public boolean getFaultDebug() {
    return this.faultDebug;
  }

  public void setNamedLoaderDebug(boolean value) {
    this.namedLoaderDebug = value;
  }

  public boolean getNamedLoaderDebug() {
    return this.namedLoaderDebug;
  }

  public void updateFaultStats(String type) {
    faultStatsRecorder.updateStats(type, StatsRecorder.SINGLE_INCR);
  }

  public void lockAcquired(String lockName, int level, Object instance, TCObject tcObject) {
    boolean isAutoLock = ByteCodeUtil.isAutolockName(lockName);

    if (isAutoLock) {
      autoLockAcquired(lockName, level, instance, tcObject);
    } else {
      namedLockAcquired(lockName, level);
    }
  }

  private void namedLockAcquired(String lockName, int level) {
    StringBuffer message = new StringBuffer("Named lock [").append(lockName).append("] acquired with level ")
        .append(LockLevel.toString(level));
    appendCall(message);
    logger.info(message);
  }

  private void autoLockAcquired(String lockName, int level, Object instance, TCObject tcObject) {
    StringBuffer message = new StringBuffer("Autolock [").append(lockName).append("] acquired with level ")
        .append(LockLevel.toString(level));

    if (autoLockDetails && (instance != null)) {
      message.append("\n  type: ").append(instance.getClass().getName());
      message.append(", identityHashCode: 0x").append(Integer.toHexString(System.identityHashCode(instance)));
    }

    appendCall(message);

    logger.info(message);
  }

  private void appendCall(StringBuffer message) {
    if (fullStack) {
      StackTraceElement[] stack = new Throwable().getStackTrace();
      if (stack != null) {
        message.append("\n");
        for (int i = 0; i < stack.length; i++) {
          message.append("  at ").append(stack[i].toString());

          if (i < (stack.length - 1)) {
            message.append("\n");
          }
        }
      }
    }

  }

  public void literalValueChanged(TCObject source, Object newValue) {
    StringBuffer message = new StringBuffer("DSO object literal value changed\n");
    if (newValue != null) {
      message.append("\n  newValue type: ").append(newValue.getClass().getName());
      message.append(", identityHashCode: 0x").append(Integer.toHexString(System.identityHashCode(newValue)));
    } else {
      message.append("\n  newValue: null");
    }

    logger.info(message.toString());
  }

  public void fieldChanged(TCObject source, String classname, String fieldName, Object newValue, int index) {
    StringBuffer message = new StringBuffer("DSO object field changed\n");
    message.append("  class: ").append(classname).append(", field: ").append(fieldName);
    if (index >= 0) {
      message.append(", index: ").append(index);
    }
    if (newValue != null) {
      message.append("\n  newValue type: ").append(newValue.getClass().getName());
      message.append(", identityHashCode: 0x").append(Integer.toHexString(System.identityHashCode(newValue)));
    } else {
      message.append("\n  newValue: null");
    }

    logger.info(message.toString());
  }

  public void arrayChanged(TCObject source, int startPos, Object array) {
    StringBuffer message = new StringBuffer("DSO array changed\n");
    message.append("\n startPos: ").append(startPos);
    message.append("\n subset component types: \n").append(array.getClass().getComponentType());

    logger.info(message.toString());
  }

  public void newManagedObject(TCObject object) {
    StringBuffer message = new StringBuffer("New DSO Object instance created\n");
    message.append("  instance: ").append(baseToString(object.getPeerObject())).append("\n");
    message.append("  object ID: ").append(object.getObjectID());
    appendCall(message);
    logger.info(message.toString());
  }

  public void objectNotify(boolean all, Object obj, TCObject tcObject) {
    StringBuffer message = new StringBuffer("notify").append(all ? "All()" : "()");
    message.append(" called on ").append(baseToString(obj)).append(", ObjectID: ").append(
                                                                                          tcObject.getObjectID()
                                                                                              .toLong());
    logger.info(message.toString());
  }

  public void objectWait(TimerSpec call, Object obj, TCObject tcObject) {
    StringBuffer message = new StringBuffer(call.toString()).append(" called on ");
    message.append(baseToString(obj)).append(", ObjectID: ").append(tcObject.getObjectID().toLong());
    logger.info(message.toString());
  }

  public void distributedMethodCall(String receiverName, String methodName, String params) {
    StringBuffer message = new StringBuffer("Distributed method invoked\n");
    message.append("  receiver class: ").append(receiverName).append("\n");
    message.append("  methodName: ").append(methodName).append("\n");
    message.append("  params: ").append(params).append("\n");

    appendCall(message);
    logger.info(message.toString());
  }

  public void distributedMethodCallError(String receiverClassName, String methodName, String params, Throwable error) {
    StringBuffer message = new StringBuffer("Unhandled execption occurred in distributed method call\n");
    message.append(" receiver class: ").append(receiverClassName).append("\n");
    message.append(" methodName: ").append(methodName).append("\n");
    message.append(" params: ").append(params).append("\n");

    logger.warn(message.toString(), error);
  }

  private static String baseToString(Object obj) {
    if (obj == null) { return null; }
    return obj.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(obj));
  }

  public void namedLoaderRegistered(NamedClassLoader loader, String name, NamedClassLoader previous) {
    StringBuffer message = new StringBuffer("loader of type [");
    message.append(loader.getClass().getName()).append("] with name [").append(name);
    message.append("] registered (replaced: ").append(previous != null).append(")");

    appendCall(message);
    logger.info(message);
  }
}
