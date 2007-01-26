/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.config;

import org.apache.commons.lang.ArrayUtils;

import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.logging.TCLogger;
import com.tc.object.config.schema.ExcludedInstrumentedClass;
import com.tc.object.config.schema.IncludeOnLoad;
import com.tc.object.config.schema.IncludedInstrumentedClass;
import com.tc.util.Assert;
import com.tc.util.ClassUtils;
import com.tc.util.ClassUtils.ClassSpec;
import com.terracottatech.configV3.AdditionalBootJarClasses;
import com.terracottatech.configV3.Autolock;
import com.terracottatech.configV3.DistributedMethods;
import com.terracottatech.configV3.DsoApplication;
import com.terracottatech.configV3.Include;
import com.terracottatech.configV3.InstrumentedClasses;
import com.terracottatech.configV3.LockLevel;
import com.terracottatech.configV3.Locks;
import com.terracottatech.configV3.NamedLock;
import com.terracottatech.configV3.NonDistributedFields;
import com.terracottatech.configV3.OnLoad;
import com.terracottatech.configV3.Root;
import com.terracottatech.configV3.Roots;
import com.terracottatech.configV3.SpringAppContext;
import com.terracottatech.configV3.SpringApplication;
import com.terracottatech.configV3.SpringApps;
import com.terracottatech.configV3.SpringBean;
import com.terracottatech.configV3.SpringDistributedEvent;
import com.terracottatech.configV3.SpringPath;
import com.terracottatech.configV3.TransientFields;
import com.terracottatech.configV3.WebApplications;

import java.text.ParseException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


public class ConfigLoader {
  private final DSOClientConfigHelper config;
  private final TCLogger logger;

  public ConfigLoader(DSOClientConfigHelper config, TCLogger logger) {
    this.config = config;
    this.logger = logger;
  }

  public void loadDsoConfig(DsoApplication dsoApplication) throws ConfigurationSetupException {
    if (dsoApplication != null) {
      Roots rootsList = dsoApplication.getRoots();
      if (rootsList != null && rootsList.getRootArray() != null) {
        Root[] roots = rootsList.getRootArray();
        for (int i = 0; i < roots.length; ++i) {
          Root root = roots[i];
          try {
            ClassSpec classSpec = ClassUtils.parseFullyQualifiedFieldName(root.getFieldName());
            String className = classSpec.getFullyQualifiedClassName();
            String fieldName = classSpec.getShortFieldName();
            String rootName = root.getRootName();
            config.addRoot(className, fieldName, rootName, false);
          } catch (ParseException pe) {
            throw new ConfigurationSetupException("Root '" + root.getFieldName() + "' is invalid", pe);
          }
        }
      }

      WebApplications webApplicationsList = dsoApplication.getWebApplications();
      if (webApplicationsList != null && webApplicationsList.getWebApplicationArray() != null) {
        String[] webApplications = webApplicationsList.getWebApplicationArray();
        for (int i = 0; i < webApplications.length; i++) {
          config.addApplicationName(webApplications[i]);
        }
      }

      loadLocks(dsoApplication.getLocks());
      loadTransientFields(dsoApplication.getTransientFields());
      loadInstrumentedClasses(dsoApplication.getInstrumentedClasses());
      loadDistributedMethods(dsoApplication.getDistributedMethods());

      AdditionalBootJarClasses additionalBootJarClassesList = dsoApplication.getAdditionalBootJarClasses();
      // XXX
      if (additionalBootJarClassesList != null) {
        Set userDefinedBootClassNames = new HashSet();
        userDefinedBootClassNames.addAll(Arrays.asList(additionalBootJarClassesList.getIncludeArray()));
        logger.debug("boot-jar/includes: " + ArrayUtils.toString(userDefinedBootClassNames));

        for (Iterator i = userDefinedBootClassNames.iterator(); i.hasNext();) {
          String className = (String) i.next();
          if (config.getSpec(className) == null) {
            TransparencyClassSpec spec = new TransparencyClassSpec(className, config);
            spec.markPreInstrumented();
            config.addUserDefinedBootSpec(spec.getClassName(), spec);
          }
        }
      }
    }
  }

  public void loadSpringConfig(SpringApplication springApplication) throws ConfigurationSetupException {
    if(springApplication!=null) {
      SpringApps[] springApps = springApplication.getJeeApplicationArray();
      for (int i = 0; springApps != null && i < springApps.length; i++) {
        SpringApps springApp = springApps[i];
        if (springApp != null) {
          loadSpringApp(springApp);
        }
      }
    }
  }
  
  private void loadSpringApp(SpringApps springApp) throws ConfigurationSetupException {
    // TODO scope the following by app namespace https://jira.terracotta.lan/jira/browse/LKC-2284
    loadLocks(springApp.getLocks());
    loadTransientFields(springApp.getTransientFields());
    loadInstrumentedClasses(springApp.getInstrumentedClasses());

    if (springApp.getSessionSupport()) {
      config.addApplicationName(springApp.getName()); // enable session support
    }

    if(springApp.getApplicationContexts()!=null) {
      loadSpringAppContexts(springApp);
    }
  }
  
  
  private void loadSpringAppContexts(SpringApps springApp) {
    String appName = springApp.getName();
    boolean fastProxy = springApp.getFastProxy();
    SpringAppContext[] applicationContexts = springApp.getApplicationContexts().getApplicationContextArray();
    for (int i = 0; applicationContexts!=null && i < applicationContexts.length; i++) {
      SpringAppContext appContext = applicationContexts[i];
      if (appContext == null) continue;

      DSOSpringConfigHelper springConfigHelper = new StandardDSOSpringConfigHelper();
      springConfigHelper.addApplicationNamePattern(appName);
      springConfigHelper.setFastProxyEnabled(fastProxy); // copy flag to all subcontexts

      SpringDistributedEvent distributedEventList = appContext.getDistributedEvents();
      if (distributedEventList != null) {
        String[] distributedEvents = distributedEventList.getDistributedEventArray();
        for (int k = 0; distributedEvents != null && k < distributedEvents.length; k++) {
          springConfigHelper.addDistributedEvent(distributedEvents[k]);
        }
      }

      SpringPath pathList = appContext.getPaths();
      if(pathList!=null) {
        String[] paths = pathList.getPathArray();
        for (int j = 0; paths!=null && j < paths.length; j++) {
          springConfigHelper.addConfigPattern(paths[j]);
        }
      }
      
      SpringBean springBean = appContext.getBeans();
      if(springBean!=null) {
        NonDistributedFields[] nonDistributedFields = springBean.getBeanArray();
        for (int j = 0; nonDistributedFields != null && j < nonDistributedFields.length; j++) {
          NonDistributedFields nonDistributedField = nonDistributedFields[j];
          
          String beanName = nonDistributedField.getName();
          springConfigHelper.addBean(beanName);

          String[] fields = nonDistributedField.getNonDistributedFieldArray();
          for (int k = 0; fields != null && k < fields.length; k++) {
            springConfigHelper.excludeField(beanName, fields[k]);
          }
        }
      }
      
      config.addDSOSpringConfig(springConfigHelper);
    }    
  }

  private ConfigLockLevel getLockLevel(LockLevel.Enum lockLevel) {
    if(lockLevel==null || LockLevel.WRITE.equals(lockLevel)) {
      return ConfigLockLevel.WRITE;
    } else if (LockLevel.CONCURRENT.equals(lockLevel)) {
      return ConfigLockLevel.CONCURRENT;
    } else if (LockLevel.READ.equals(lockLevel)) {
      return ConfigLockLevel.READ;
    }
    throw Assert.failure("Unknown lock level " + lockLevel);
  }
  
  private void loadLocks(Locks lockList) {
    if (lockList == null) return;

    Autolock[] autolocks = lockList.getAutolockArray();
    for (int i = 0; autolocks!=null && i < autolocks.length; i++) {
      config.addAutolock(autolocks[i].getMethodExpression(), getLockLevel(autolocks[i].getLockLevel()));
    }
    
    NamedLock[] namedLocks = lockList.getNamedLockArray();
    for (int i = 0; namedLocks!=null && i < namedLocks.length; i++) {
      NamedLock namedLock = namedLocks[i];
      LockDefinition lockDefinition = new LockDefinition(namedLock.getLockName(), getLockLevel(namedLock.getLockLevel()));
      lockDefinition.commit();
      config.addLock(namedLock.getMethodExpression(), lockDefinition);
    }
  }

  private void loadTransientFields(TransientFields transientFieldsList) throws ConfigurationSetupException {
    if (transientFieldsList != null) {
      String[] transientFields = transientFieldsList.getFieldNameArray();
      try {
        for (int i = 0; transientFields != null && i < transientFields.length; i++) {
          ClassSpec spec = ClassUtils.parseFullyQualifiedFieldName(transientFields[i]);
          config.addTransient(spec.getFullyQualifiedClassName(), spec.getShortFieldName());
        }
      } catch (ParseException e) {
        throw new ConfigurationSetupException(e.getLocalizedMessage(), e);
      }
    }
  }

  private void loadInstrumentedClasses(InstrumentedClasses instrumentedClasses) {
    if (instrumentedClasses != null) {
      Include[] includes = instrumentedClasses.getIncludeArray();
      for (int i = 0; includes != null && i < includes.length; i++) {
        Include include = includes[i];
        IncludeOnLoad includeOnLoad = null;
        OnLoad onLoad = include.getOnLoad();
        if(onLoad!=null) {
          if(onLoad.getExecute()!=null) {
            includeOnLoad = new IncludeOnLoad(IncludeOnLoad.EXECUTE, onLoad.getExecute());
          } else if(onLoad.getMethod()!=null) {
            includeOnLoad = new IncludeOnLoad(IncludeOnLoad.METHOD, onLoad.getMethod());
          }
        }
        config.addInstrumentationDescriptor(new IncludedInstrumentedClass(include.getClassExpression(), include
            .getHonorTransient(), false, includeOnLoad));
      }
      
      String[] excludeArray = instrumentedClasses.getExcludeArray();
      for (int i = 0; excludeArray!=null && i < excludeArray.length; i++) {
        config.addInstrumentationDescriptor(new ExcludedInstrumentedClass(excludeArray[i]));
      }
    }
  }
  
  private void loadDistributedMethods(DistributedMethods distributedMethods) {
    if(distributedMethods!=null) {
      String[] methodExpressions = distributedMethods.getMethodExpressionArray();
      for (int i = 0; methodExpressions != null && i < methodExpressions.length; i++) {
        config.addDistributedMethodCall(methodExpressions[i]);
      }
    }
  }
  
}

