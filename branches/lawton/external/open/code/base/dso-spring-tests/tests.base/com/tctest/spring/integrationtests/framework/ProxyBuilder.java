/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest.spring.integrationtests.framework;

import java.util.Map;

public interface ProxyBuilder {
  public static final String EXPORTER_TYPE_KEY = "exporter-type";
  public static final String HTTP_CLIENT_KEY = "http-client";
  
  public Object createProxy(Class serviceType, String url, Map initialContext) throws Exception;
}
