/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.plugins.iBatis_2_2_0.object.config;

import org.terracotta.plugins.iBatis_2_2_0.object.dna.impl.IBatisAccessPlanInstance;

import com.tc.object.config.ChangeApplicatorSpec;
import com.tc.object.config.PluginSpec;

public class IBatisPluginSpec implements PluginSpec {
  private final ChangeApplicatorSpec changeAppSpec;
  
  public IBatisPluginSpec(ChangeApplicatorSpec changeAppSpec) {
    this.changeAppSpec = changeAppSpec;
  }
  
  public ChangeApplicatorSpec getChangeApplicatorSpec() {
    return this.changeAppSpec;
  }

  public Class getPeerClass(Class clazz) {
    if (isIBatisAccessPlan(clazz)) {
      return IBatisAccessPlanInstance.class;
    }
    return clazz;
  }

  public boolean isUseNonDefaultConstructor(Class clazz) {
    return isIBatisAccessPlan(clazz);
  }
  
  public boolean isPortableClass(Class clazz) {
    return isIBatisAccessPlan(clazz);
  }
  
  private static boolean isIBatisAccessPlan(Class clazz) {
    if (IBatisAccessPlanInstance.class.getName().equals(clazz.getName())) { return true; }
    Class superclass = clazz.getSuperclass();
    if (superclass == null) { return false; }
    if (superclass.getName().equals("com.ibatis.sqlmap.engine.accessplan.BaseAccessPlan")) { return true; }
    return false;

  }

}
