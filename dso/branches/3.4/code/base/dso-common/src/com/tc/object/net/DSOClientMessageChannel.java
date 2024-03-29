/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.net;

import com.tc.async.api.Sink;
import com.tc.management.lock.stats.LockStatisticsReponseMessageFactory;
import com.tc.net.CommStackMismatchException;
import com.tc.net.GroupID;
import com.tc.net.MaxConnectionsExceededException;
import com.tc.net.protocol.tcm.ChannelEventListener;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.net.protocol.tcm.GeneratedMessageFactory;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.ClientIDProvider;
import com.tc.object.msg.AcknowledgeTransactionMessageFactory;
import com.tc.object.msg.ClientHandshakeMessageFactory;
import com.tc.object.msg.CommitTransactionMessageFactory;
import com.tc.object.msg.CompletedTransactionLowWaterMarkMessageFactory;
import com.tc.object.msg.JMXMessage;
import com.tc.object.msg.KeysForOrphanedValuesMessageFactory;
import com.tc.object.msg.LockRequestMessageFactory;
import com.tc.object.msg.NodeMetaDataMessageFactory;
import com.tc.object.msg.NodesWithObjectsMessageFactory;
import com.tc.object.msg.ObjectIDBatchRequestMessageFactory;
import com.tc.object.msg.RequestManagedObjectMessageFactory;
import com.tc.object.msg.RequestRootMessageFactory;
import com.tc.object.msg.ServerMapMessageFactory;
import com.tc.util.TCTimeoutException;

import java.io.IOException;
import java.net.UnknownHostException;

public interface DSOClientMessageChannel {

  public void addClassMapping(TCMessageType messageType, Class messageClass);

  public void addClassMapping(TCMessageType messageType, GeneratedMessageFactory generatedMessageFactory);

  public ClientIDProvider getClientIDProvider();

  public void addListener(ChannelEventListener listener);

  public void routeMessageType(TCMessageType messageType, Sink destSink, Sink hydrateSink);

  public void open() throws MaxConnectionsExceededException, TCTimeoutException, UnknownHostException, IOException,
      CommStackMismatchException;

  public boolean isConnected();

  public void close();

  public ClientMessageChannel channel();

  public LockRequestMessageFactory getLockRequestMessageFactory();

  public CompletedTransactionLowWaterMarkMessageFactory getCompletedTransactionLowWaterMarkMessageFactory();

  public RequestRootMessageFactory getRequestRootMessageFactory();

  public RequestManagedObjectMessageFactory getRequestManagedObjectMessageFactory();

  public ServerMapMessageFactory getServerMapMessageFactory();

  public ObjectIDBatchRequestMessageFactory getObjectIDBatchRequestMessageFactory();

  public CommitTransactionMessageFactory getCommitTransactionMessageFactory();

  public ClientHandshakeMessageFactory getClientHandshakeMessageFactory();

  public AcknowledgeTransactionMessageFactory getAcknowledgeTransactionMessageFactory();

  public NodesWithObjectsMessageFactory getNodesWithObjectsMessageFactory();

  public KeysForOrphanedValuesMessageFactory getKeysForOrphanedValuesMessageFactory();

  public NodeMetaDataMessageFactory getNodeMetaDataMessageFactory();

  public LockStatisticsReponseMessageFactory getLockStatisticsReponseMessageFactory();

  public JMXMessage getJMXMessage();

  public GroupID[] getGroupIDs();
}
