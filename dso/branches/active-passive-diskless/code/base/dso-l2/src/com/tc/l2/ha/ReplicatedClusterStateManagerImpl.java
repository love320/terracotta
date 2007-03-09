/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.ha;

import com.tc.l2.api.ReplicatedClusterStateManager;
import com.tc.l2.msg.ClusterStateMessage;
import com.tc.l2.msg.ClusterStateMessageFactory;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.groups.GroupException;
import com.tc.net.groups.GroupManager;
import com.tc.net.groups.GroupMessage;
import com.tc.net.groups.GroupMessageListener;
import com.tc.net.groups.GroupResponse;
import com.tc.net.groups.NodeID;
import com.tc.util.sequence.ObjectIDSequence;

import java.util.Iterator;

public class ReplicatedClusterStateManagerImpl implements ReplicatedClusterStateManager, GroupMessageListener {

  private static final TCLogger  logger = TCLogging.getLogger(ReplicatedClusterStateManagerImpl.class);

  private final GroupManager     groupManager;
  private final ClusterState     state  = new ClusterState();

  private final ObjectIDSequence oidSequenceProvider;

  public ReplicatedClusterStateManagerImpl(GroupManager groupManager, ObjectIDSequence oidSequenceProvider) {
    this.groupManager = groupManager;
    this.oidSequenceProvider = oidSequenceProvider;
    groupManager.registerForMessages(ClusterStateMessage.class, this);
  }

  public void sync() {
    // Sync state to internal DB
    syncOIDSequence();

    // Sync state to external passive servers
    // TODO::Is this needed ?
  }

  private void syncOIDSequence() {
    if (state.getNextAvailableObjectID() != -1) {
      this.oidSequenceProvider.setNextAvailableObjectID(state.getNextAvailableObjectID());
    }
  }

  // TODO:: Sync only once a while to the passives
  public void publishNextAvailableObjectID(long maxID) {
    state.setNextAvailableObjectID(maxID);
    try {
      GroupResponse gr = groupManager.sendAllAndWaitForResponse(ClusterStateMessageFactory
          .createNextAvailableObjectIDMessage(state));
      for (Iterator i = gr.getResponses().iterator(); i.hasNext();) {
        ClusterStateMessage msg = (ClusterStateMessage) i.next();
        if (msg.getType() != ClusterStateMessage.OPERATION_SUCCESS) {
          logger.error("Recd wrong response from : " + msg.messageFrom() + " : msg = " + msg
                       + " while publishing Next Available ObjectID: Killing the node");
          groupManager.zapNode(msg.messageFrom());
        }
      }
    } catch (GroupException e) {
      throw new AssertionError(e);
    }
  }

  public void messageReceived(NodeID fromNode, GroupMessage msg) {
    if (msg instanceof ClusterStateMessage) {
      ClusterStateMessage clusterMsg = (ClusterStateMessage) msg;
      handleClusterStateMessage(fromNode, clusterMsg);
    } else {
      throw new AssertionError("ReplicatedClusterStateManagerImpl : Received wrong message type :"
                               + msg.getClass().getName() + " : " + msg);

    }
  }

  private void handleClusterStateMessage(NodeID fromNode, ClusterStateMessage msg) {
    try {
      switch (msg.getType()) {
        case ClusterStateMessage.OBJECT_ID:
          state.setNextAvailableObjectID(msg.getClusterState().getNextAvailableObjectID());
          sendOKResponse(fromNode, msg);
          break;
        default:
          throw new AssertionError("This message shouldn't have been routed here : " + msg);
      }
    } catch (GroupException e) {
      logger.error("Error handling message : " + msg, e);
      throw new AssertionError(e);
    }
  }

  private void sendOKResponse(NodeID fromNode, ClusterStateMessage msg) throws GroupException {
    groupManager.sendTo(fromNode, ClusterStateMessageFactory.createOKResponse(msg));
  }

  public static final class ClusterState {

    private long nextAvailObjectID = -1;

    public void setNextAvailableObjectID(long nextAvailOID) {
      if (nextAvailOID < nextAvailObjectID) {
        // Could happen when two actives fight it out. Dont want to assert, let the state manager fight it out.
        logger.error("Trying to set Next Available ObjectID to a lesser value : known = " + nextAvailObjectID
                     + " new value = " + nextAvailOID + " IGNORING");
        return;
      }
      this.nextAvailObjectID = nextAvailOID;
    }

    public long getNextAvailableObjectID() {
      return nextAvailObjectID;
    }
  }
}
