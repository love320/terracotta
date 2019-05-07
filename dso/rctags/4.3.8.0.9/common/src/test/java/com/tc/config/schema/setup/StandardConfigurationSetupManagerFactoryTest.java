package com.tc.config.schema.setup;

import org.junit.Test;

import com.tc.config.schema.IllegalConfigurationChangeHandler;
import com.tc.security.PwProvider;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class StandardConfigurationSetupManagerFactoryTest {

  @Test
  public void testSafeModeConfiguration() throws Exception {
    String[] args = {"-f", this.getClass().getResource("/default-config.xml").getPath(), "--safe-mode"};

    StandardConfigurationSetupManagerFactory factory =
        new StandardConfigurationSetupManagerFactory(args,
                                                     StandardConfigurationSetupManagerFactory.ConfigMode.L2,
                                                     mock(IllegalConfigurationChangeHandler.class),
                                                     mock(PwProvider.class));

    L2ConfigurationSetupManager l2TVSConfigurationSetupManager =
        factory.createL2TVSConfigurationSetupManager(null, false);

    assertTrue(l2TVSConfigurationSetupManager.isSafeModeConfigured());
  }

  @Test
  public void testNonSafeModeConfiguration() throws Exception {
    String[] args = {"-f", this.getClass().getResource("/default-config.xml").getPath()};

    StandardConfigurationSetupManagerFactory factory =
        new StandardConfigurationSetupManagerFactory(args,
                                                     StandardConfigurationSetupManagerFactory.ConfigMode.L2,
                                                     mock(IllegalConfigurationChangeHandler.class),
                                                     mock(PwProvider.class));

    L2ConfigurationSetupManager l2TVSConfigurationSetupManager =
        factory.createL2TVSConfigurationSetupManager(null, false);

    assertFalse(l2TVSConfigurationSetupManager.isSafeModeConfigured());
  }
}