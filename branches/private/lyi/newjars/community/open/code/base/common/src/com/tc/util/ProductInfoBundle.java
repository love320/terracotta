/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.util;

import java.util.ListResourceBundle;

public class ProductInfoBundle extends ListResourceBundle {
  public Object[][] getContents() {
    return contents;
  }

  static final Object[][] contents = {
    {"moniker", "Terracotta"},
    {"invalid.timestamp", "The build timestamp string ''${0}'' does not appear to be valid."},
    {"load.properties.failure", "Unable to load build properties from ''${0}''."},
    {"copyright", "Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved."},
    {"option.verbose", "Produces more detailed information."},
    {"option.help", "Shows this text."}
  };
}
