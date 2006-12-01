/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.Type;
import com.tc.exception.TCLogicalSubclassNotPortableException;
import com.tc.object.LiteralValues;
import com.tc.object.Portability;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.config.TransparencyClassSpecUtil;
import com.tc.util.Assert;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class InstrumentationSpec {
  public static final byte            IS_NOT_NEEDED               = 0x04;
  public static final byte            IS_NEEDED                   = 0x05;
  public static final byte            IS_PRESENT                  = 0x06;

  private static final LiteralValues  literalValues               = new LiteralValues();

  private byte                        instrumentationAction       = TransparencyClassSpec.NOT_ADAPTABLE;

  private byte                        managedMethods              = IS_NOT_NEEDED;
  private byte                        valuesGetterMethod          = IS_NOT_NEEDED;
  private byte                        valuesSetterMethod          = IS_NOT_NEEDED;
  private byte                        managedValuesGetterMethod   = IS_NOT_NEEDED;
  private byte                        managedValuesSetterMethod   = IS_NOT_NEEDED;
  private byte                        managedField                = IS_NOT_NEEDED;
  private byte                        delegateLogicalField        = IS_NOT_NEEDED;
  private byte                        writeObjectSerializedMethod = IS_NOT_NEEDED;
  private byte                        readObjectSerializedMethod  = IS_NOT_NEEDED;

  private String                      classNameSlashes;
  private String                      superNameSlashes;
  private String                      classNameDots;
  private String                      superNameDots;
  private boolean                     isInterface;
  private ParentClassInfo             parentClassInfo;
  private boolean                     classHierarchyInitialized   = false;
  private int                         classAccess;
  private String                      classSignature;
  private String[]                    classInterfaces;
  private int                         classVersion;
  private boolean                     hasVisitedField;
  private boolean                     isSubclassOfLogicalClass;
  private TransparencyClassSpec       superClassSpec;

  private final ClassLoader           caller;
  private final TransparencyClassSpec spec;
  private final ManagerHelper         mgrHelper;
  private final Set                   classHierarchy;
  private final Map                   shouldOverrideMethods;
  private final Set                   logicalExtendingMethodSpec;
  private final Set                   logicalExtendingFieldSpec;

  InstrumentationSpec(TransparencyClassSpec spec, ManagerHelper mgrHelper, ClassLoader caller) {
    this.spec = spec;
    this.mgrHelper = mgrHelper;
    this.caller = caller;
    this.classHierarchy = new HashSet();
    this.shouldOverrideMethods = new HashMap();
    this.logicalExtendingMethodSpec = new HashSet();
    this.logicalExtendingFieldSpec = new HashSet();
  }

  void initialize(int version, int access, String name, String signature, String superName, String[] interfaces,
                  Portability portability) {
    this.classNameSlashes = name;
    this.superNameSlashes = superName;
    this.classNameDots = name.replace('/', '.');
    this.superNameDots = superName.replace('/', '.');
    this.classHierarchy.add(this.classNameSlashes);
    this.classHierarchy.add(this.superNameSlashes);
    this.classAccess = access;
    this.classSignature = signature;
    this.classInterfaces = interfaces;
    this.classVersion = version;
    handleSubclassOfLogicalClass(access, classNameDots, superNameDots);
    decideOnInstrumentationAction(portability);
  }

  private boolean isArray(String className) {
    return literalValues.valueForClassName(className) == LiteralValues.ARRAY;
  }

  private void handleSubclassOfLogicalClass(int access, String className, String superName) {
    if (isLogical()) { return; }
    if (isArray(className)) { return; }
    if (TransparencyClassSpecUtil.ignoreChecks(className)) { return; }
    if (TransparencyClassSpecUtil.ignoreChecks(superName)) { return; }

    superClassSpec = spec.getClassSpec(superName);
    if (superClassSpec != null && superClassSpec.isLogical()) {
      isSubclassOfLogicalClass = true;
    }
  }

  public int getClassVersion() {
    return classVersion;
  }

  public int getClassAccess() {
    return classAccess;
  }

  private void initClassHierarchy() {
    String superClassName = superNameSlashes.replace('/', '.');
    try {
      // As long as we are instrumenting anything other than Object, we are fine.
      Class superClazz = Class.forName(superClassName, false, this.caller).getSuperclass();
      while (superClazz != null) {
        String superName = superClazz.getName();
        classHierarchy.add(superName.replace('.', '/'));
        superClazz = superClazz.getSuperclass();
      }
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    } finally {
      classHierarchyInitialized = true;
    }
  }

  String getClassNameSlashes() {
    return this.classNameSlashes;
  }

  String getClassNameDots() {
    return this.classNameDots;
  }

  String getSuperClassNameSlashes() {
    return this.superNameSlashes;
  }

  String getSuperClassNameDots() {
    return this.superNameDots;
  }

  String[] getClassInterfaces() {
    return classInterfaces;
  }

  String getClassSignature() {
    return classSignature;
  }

  void decideOnInstrumentationAction(Portability portability) {
    Assert.assertNotNull(classNameSlashes);
    Assert.assertNotNull(superNameSlashes);

    this.isInterface = Modifier.isInterface(classAccess);

    if (isInterface) {
      this.instrumentationAction = TransparencyClassSpec.NOT_ADAPTABLE;
    } else if (spec.getInstrumentationAction() == TransparencyClassSpec.PORTABLE) {
      this.instrumentationAction = TransparencyClassSpec.PORTABLE;
    } else if (spec.getInstrumentationAction() == TransparencyClassSpec.ADAPTABLE) {
      this.instrumentationAction = TransparencyClassSpec.ADAPTABLE;
    } else if (spec.isLogical() || spec.ignoreChecks()) {
      // Logically managed classes need not have all super classes instrumented.
      // currently THashMap and THashSet are not in bootjar and are instrumented during runtime.
      this.instrumentationAction = TransparencyClassSpec.PORTABLE;
    } else if (superClassChecks(portability)) {
      this.instrumentationAction = TransparencyClassSpec.ADAPTABLE;
    } else {
      this.instrumentationAction = TransparencyClassSpec.PORTABLE;
    }
    decideOnInstrumentationsToDo(portability);
  }

  private void decideOnInstrumentationsToDo(Portability portability) {
    if (this.instrumentationAction == TransparencyClassSpec.PORTABLE) {
      if (!isLogical()) {
        valuesGetterMethod = IS_NEEDED;
        valuesSetterMethod = IS_NEEDED;
        managedValuesGetterMethod = IS_NEEDED;
        managedValuesSetterMethod = IS_NEEDED;
      }
      managedField = isNeedManagedField(portability);
      managedMethods = managedField;
    }
  }

  /**
   * If the superclass does not need to be instrumented, we need to generate the managed field and method for the class.
   * If there does not exist a portable specs for the superclass, we need to generate the managed field and method for
   * the class. If the class is logically instrumented, we need to generate the managed field and method. If it is a
   * subclass of a logically instrumented class, we do not need to generate the field.
   */
  private byte isNeedManagedField(Portability portability) {
    if (isSubclassOfLogicalClass) { return IS_NOT_NEEDED; }
    String superClassName = superNameSlashes.replace('/', '.');
    if (portability.isInstrumentationNotNeeded(superClassName)) { return IS_NEEDED; }
    if (!spec.hasPhysicallyPortableSpecs(superClassName)) { return IS_NEEDED; }
    if (spec.isLogical()) { return IS_NEEDED; }
    return IS_NOT_NEEDED;
  }

  private boolean superClassChecks(Portability portability) {
    String superClassName = superNameSlashes.replace('/', '.');
    if (portability.isInstrumentationNotNeeded(superClassName)) { return false; }

    final Class superClazz;
    try {
      superClazz = Class.forName(superClassName, false, this.caller);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }

    // if (!portability.isPortableClass(superClazz) || !portability.allowSubclassOf(superClazz)) { return true; }
    if (!portability.isPortableClass(superClazz)) { return true; }
    return false;
  }

  boolean isInClassHierarchy(String classname) {
    boolean inClassHierarchy = classHierarchy.contains(classname);
    if (inClassHierarchy || classHierarchyInitialized) return inClassHierarchy;
    initClassHierarchy();
    return classHierarchy.contains(classname);
  }

  boolean isClassNotAdaptable() {
    return this.instrumentationAction == TransparencyClassSpec.NOT_ADAPTABLE;
  }

  boolean isClassAdaptable() {
    return this.instrumentationAction == TransparencyClassSpec.ADAPTABLE;
  }

  boolean isClassPortable() {
    return this.instrumentationAction == TransparencyClassSpec.PORTABLE;
  }

  boolean isSubclassofLogicalClass() {
    return this.isSubclassOfLogicalClass;
  }

  void moveToLogicalIfNecessary() {
    if (isSubclassOfLogicalClass && !hasVisitedField) {
      spec.moveToLogical(superClassSpec);
    }
  }

  boolean hasDelegatedToLogicalClass() {
    // return (isSubclassOfLogicalClass && hasVisitedField);
    return needDelegateField();
  }

  boolean needDelegateField() {
    return this.delegateLogicalField == IS_NEEDED;
  }

  boolean isWriteObjectMethodNeeded() {
    return this.writeObjectSerializedMethod == IS_NEEDED;
  }

  boolean isReadObjectMethodNeeded() {
    return this.readObjectSerializedMethod == IS_NEEDED;
  }

  void handleSubclassOfLogicalClassWithFieldsIfNecessary(int access) {
    if (ByteCodeUtil.isSynthetic(access) || Modifier.isStatic(access)) {
      return;
    } else if (isSubclassOfLogicalClass && !hasVisitedField) {
      hasVisitedField = true;
      try {
        Class superClazz = Class.forName(superNameDots, false, caller);

        Method[] methods = superClazz.getMethods();
        for (int i = 0; i < methods.length; i++) {
          Method m = methods[i];
          String methodName = m.getName();
          int modifier = m.getModifiers();
          if (!shouldVisitMethod(modifier, methodName) || Modifier.isFinal(modifier)) {
            continue;
          }

          String methodDesc = Type.getMethodDescriptor(m);
          shouldOverrideMethods.put(methodName + methodDesc, m);
          // logicalExtendingMethodSpec.add(methodName + methodDesc);
        }

        methods = superClazz.getDeclaredMethods();
        for (int i = 0; i < methods.length; i++) {
          Method m = methods[i];
          String methodName = m.getName();
          int modifier = m.getModifiers();
          if (shouldVisitMethod(modifier, methodName) && !Modifier.isFinal(modifier) && Modifier.isProtected(modifier)) {
            String methodDesc = Type.getMethodDescriptor(m);
            logicalExtendingMethodSpec.add(methodName + methodDesc);
          }
        }

        Field[] fields = superClazz.getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
          Field f = fields[i];
          String fieldName = f.getName();
          int modifier = f.getModifiers();
          if (!shouldVisitField(fieldName) || Modifier.isFinal(modifier) || Modifier.isPrivate(modifier)) {
            continue;
          }
          String fieldDesc = Type.getDescriptor(f.getType());
          logicalExtendingFieldSpec.add(fieldName + fieldDesc);
        }
      } catch (ClassNotFoundException e) {
        e.printStackTrace();
      }
      delegateLogicalField = IS_NEEDED;
      writeObjectSerializedMethod = IS_NEEDED;
      readObjectSerializedMethod = IS_NEEDED;
    }
  }

  void recordExistingFields(String name, String desc, String signature) {
    if (ClassAdapterBase.MANAGED_FIELD_NAME.equals(name)) {
      managedField = IS_PRESENT;
    } else if (ByteCodeUtil.isParent(name)) {
      Assert.assertNull(parentClassInfo);
      this.parentClassInfo = new ParentClassInfo(name, desc);
    }

  }

  void recordExistingMethods(String name, String desc, String signature) {
    if (ClassAdapterBase.MANAGED_METHOD.equals(name)) {
      managedMethods = IS_PRESENT;
    } else if (ClassAdapterBase.VALUES_GETTER.endsWith(name)) {
      valuesGetterMethod = IS_PRESENT;
    } else if (ClassAdapterBase.VALUES_SETTER.endsWith(name)) {
      valuesSetterMethod = IS_PRESENT;
    } else if (ClassAdapterBase.MANAGED_VALUES_GETTER.endsWith(name)) {
      managedValuesGetterMethod = IS_PRESENT;
    } else if (ClassAdapterBase.MANAGED_VALUES_SETTER.endsWith(name)) {
      managedValuesSetterMethod = IS_PRESENT;
    } else if (LogicalClassSerializationAdapter.READ_OBJECT_SIGNATURE.equals(name + desc)) {
      readObjectSerializedMethod = IS_NOT_NEEDED;
    } else if (LogicalClassSerializationAdapter.WRITE_OBJECT_SIGNATURE.equals(name + desc)) {
      writeObjectSerializedMethod = IS_NOT_NEEDED;
    }
    shouldOverrideMethods.remove(name + desc);
  }

  boolean shouldVisitField(String name) {
    return !(name.startsWith(ByteCodeUtil.TC_FIELD_PREFIX));
  }

  boolean shouldVisitMethod(int methodAccess, String name) {
    if (name.startsWith(ByteCodeUtil.TC_METHOD_PREFIX)) { return false; }
    if (Modifier.isAbstract(methodAccess) || Modifier.isNative(methodAccess)) { return false; }
    return true;
  }

  boolean isManagedMethodsNeeded() {
    return (managedMethods == IS_NEEDED);
  }

  boolean isValuesGetterMethodNeeded() {
    return (valuesGetterMethod == IS_NEEDED);
  }

  boolean isValuesSetterMethodNeeded() {
    return (valuesSetterMethod == IS_NEEDED);
  }

  boolean isManagedValuesGetterMethodNeeded() {
    return (managedValuesGetterMethod == IS_NEEDED);
  }

  boolean isManagedValuesSetterMethodNeeded() {
    return (managedValuesSetterMethod == IS_NEEDED);
  }

  boolean isManagedFieldNeeded() {
    return (managedField == IS_NEEDED);
  }

  TransparencyClassSpec getTransparencyClassSpec() {
    return spec;
  }

  TransparencyClassSpec getSuperclassTransparencyClassSpec() {
    return spec.getClassSpec(superNameDots);
  }

  boolean isLogical() {
    return spec.isLogical();
  }

  void shouldProceedInstrumentation(int access, String name, String desc) {
    if (isSubclassOfLogicalClass && shouldVisitMethod(access, name) && Modifier.isProtected(access)
        && logicalExtendingMethodSpec.contains(name + desc)) {
      // Subclass of Logical class cannot override protected method. So, ignore all instrumentation.
      throw new TCLogicalSubclassNotPortableException(classNameDots, superNameDots);
    }
  }

  void shouldProceedInstrumentation(String fieldName, String fieldDesc) {
    if (isSubclassOfLogicalClass && shouldVisitField(fieldName)
        && logicalExtendingFieldSpec.contains(fieldName + fieldDesc)) { throw new TCLogicalSubclassNotPortableException(
                                                                                                                        classNameDots,
                                                                                                                        superNameDots); }
  }

  boolean isPhysical() {
    return spec.isPhysical();
  }

  boolean generateNonStaticTCFields() {
    return spec.generateNonStaticTCFields();
  }

  Collection getShouldOverrideMethods() {
    return shouldOverrideMethods.values();
  }

  ManagerHelper getManagerHelper() {
    return mgrHelper;
  }

  boolean isInner() {
    return this.parentClassInfo != null;
  }

  String getParentClassType() {
    Assert.assertTrue(isInner());
    return parentClassInfo.getType();
  }

  String getParentClassFieldName() {
    Assert.assertTrue(isInner());
    return parentClassInfo.getFieldName();
  }

  private static class ParentClassInfo {
    private final String type;
    private final String fieldName;

    ParentClassInfo(String fieldName, String type) {
      this.fieldName = fieldName;
      this.type = type;
    }

    String getFieldName() {
      return fieldName;
    }

    String getType() {
      return type;
    }

  }
}
