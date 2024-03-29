/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema;

public interface ActiveServerGroupConfig extends NewConfig {
  MembersConfig getMembers();

  boolean isMember(String l2Name);

  NewHaConfig getHa();

  int getGroupId();
  
  String getGroupName();
}
