/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.net.protocol.tcm;

import com.tc.async.api.EventContext;

import java.util.Date;

public interface ChannelEvent extends EventContext {

  public MessageChannel getChannel();

  public Date getTimestamp();

  public ChannelEventType getType();

  public ChannelID getChannelID();

}