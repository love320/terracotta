/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.net;

import com.tc.exception.ImplementMe;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.msg.BatchTransactionAcknowledgeMessage;
import com.tc.object.msg.ClientHandshakeAckMessage;

import java.util.Collection;

public class TestDSOChannelManager implements DSOChannelManager {

  private final MessageChannel[] allChannels = new MessageChannel[0];

  public void closeAll(Collection channelIDs) {
    throw new ImplementMe();
  }

  public Collection getAllChannelIDs() {
    throw new ImplementMe();
  }

  public MessageChannel getChannel(ChannelID id) {
    throw new ImplementMe();
  }

  public String getChannelAddress(ChannelID channelID) {
    throw new ImplementMe();
  }

  public MessageChannel[] getChannels() {
    return allChannels ;
  }

  public boolean isValidID(ChannelID channelID) {
    throw new ImplementMe();
  }

  public BatchTransactionAcknowledgeMessage newBatchTransactionAcknowledgeMessage(ChannelID channelID) {
    throw new ImplementMe();
  }

  public ClientHandshakeAckMessage newClientHandshakeAckMessage(ChannelID channelID) {
    throw new ImplementMe();
  }

}
