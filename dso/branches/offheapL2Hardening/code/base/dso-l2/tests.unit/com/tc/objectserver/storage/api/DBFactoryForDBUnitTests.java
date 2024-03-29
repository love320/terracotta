/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.api;

import com.tc.config.schema.setup.L2ConfigurationSetupManager;
import com.tc.management.beans.object.ServerDBBackupMBean;
import com.tc.objectserver.storage.berkeleydb.BerkeleyDBFactory;
import com.tc.stats.counter.CounterManagerImpl;
import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.stats.counter.sampled.SampledCounterConfig;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

public class DBFactoryForDBUnitTests implements DBFactory {
  // Just change DB factory here and run tests

  private final DBFactory      dbFactory;
  private final SampledCounter faultFromDisk;

  public DBFactoryForDBUnitTests(final Properties prop) {
    dbFactory = new BerkeleyDBFactory(prop);
    CounterManagerImpl sampledCounterManager = new CounterManagerImpl();
    final SampledCounterConfig sampledCounterConfig = new SampledCounterConfig(1, 300, true, 0L);
    faultFromDisk = (SampledCounter) sampledCounterManager.createCounter(sampledCounterConfig);
  }

  public DBEnvironment createEnvironment(boolean paranoid, File envHome, SampledCounter l2FaultFromDisk)
      throws IOException {
    return dbFactory.createEnvironment(paranoid, envHome, faultFromDisk);
  }

  public ServerDBBackupMBean getServerDBBackupMBean(L2ConfigurationSetupManager configurationSetupManager) {
    return null;
  }
}
