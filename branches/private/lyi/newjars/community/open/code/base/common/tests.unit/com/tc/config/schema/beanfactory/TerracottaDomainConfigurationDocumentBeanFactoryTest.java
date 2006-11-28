/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.config.schema.beanfactory;

import org.apache.xmlbeans.XmlException;

import com.tc.config.schema.test.TerracottaConfigBuilder;
import com.tc.test.TCTestCase;
import com.terracottatech.configV2.TcConfigDocument;

import java.io.ByteArrayInputStream;

/**
 * Unit test for {@link TerracottaDomainConfigurationDocumentBeanFactory}.
 */
public class TerracottaDomainConfigurationDocumentBeanFactoryTest extends TCTestCase {

  private TerracottaDomainConfigurationDocumentBeanFactory factory;

  public void setUp() throws Exception {
    this.factory = new TerracottaDomainConfigurationDocumentBeanFactory();
  }

  public void testNormal() throws Exception {
    TerracottaConfigBuilder builder = TerracottaConfigBuilder.newMinimalInstance();
    builder.getClient().setLogs("foobar");
    byte[] xml = builder.toString().getBytes();
    ByteArrayInputStream stream = new ByteArrayInputStream(xml);

    BeanWithErrors beanWithErrors = this.factory.createBean(stream, "from test");
    assertEquals(0, beanWithErrors.errors().length);
    assertEquals("foobar", ((TcConfigDocument) beanWithErrors.bean()).getTcConfig().getClients().getLogs());
  }

  public void testXmlMisparse() throws Exception {
    TerracottaConfigBuilder builder = TerracottaConfigBuilder.newMinimalInstance();
    builder.getClient().setLogs("foo <funk>"); // an unclosed tag; the builder intentionally doesn't escape text
    byte[] xml = builder.toString().getBytes();
    ByteArrayInputStream stream = new ByteArrayInputStream(xml);

    try {
      this.factory.createBean(stream, "from test");
      fail("Didn't get XmlException on invalid XML");
    } catch (XmlException xmle) {
      // ok
    }
  }

  public void testSchemaViolation() throws Exception {
    TerracottaConfigBuilder builder = TerracottaConfigBuilder.newMinimalInstance();
    builder.getSystem().setLicenseType("funkiness"); // invalid enumeration value
    builder.getSystem().setLicenseLocation("foo");
    byte[] xml = builder.toString().getBytes();
    ByteArrayInputStream stream = new ByteArrayInputStream(xml);

    BeanWithErrors beanWithErrors = this.factory.createBean(stream, "from test");
    assertEquals(1, beanWithErrors.errors().length);
    assertEquals("from test", beanWithErrors.errors()[0].getSourceName());
    assertTrue(beanWithErrors.bean() instanceof TcConfigDocument);
  }
}
