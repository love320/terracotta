/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.object.msg.AcknowledgeTransactionMessage;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.tx.ServerTransactionManager;

/**
 * @author steve
 */
public class TransactionAcknowledgementHandler extends AbstractEventHandler {
  private ServerTransactionManager transactionManager;

  public void handleEvent(EventContext context) {
    AcknowledgeTransactionMessage atm = (AcknowledgeTransactionMessage) context;
    transactionManager.acknowledgement(atm.getRequesterID(), atm.getRequestID(), atm.getChannelID());
  }

  public void initialize(ConfigurationContext context) {
    super.initialize(context);
    ServerConfigurationContext scc = (ServerConfigurationContext) context;
    this.transactionManager = scc.getTransactionManager();
  }

}