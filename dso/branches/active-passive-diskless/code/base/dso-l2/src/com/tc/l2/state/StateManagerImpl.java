/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.state;

import EDU.oswego.cs.dl.util.concurrent.CopyOnWriteArrayList;

import com.tc.async.api.Sink;
import com.tc.l2.context.StateChangedEvent;
import com.tc.l2.msg.ClusterStateMessage;
import com.tc.l2.msg.ClusterStateMessageFactory;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.groups.GroupEventsListener;
import com.tc.net.groups.GroupException;
import com.tc.net.groups.GroupManager;
import com.tc.net.groups.GroupMessage;
import com.tc.net.groups.GroupMessageListener;
import com.tc.net.groups.NodeID;
import com.tc.util.Assert;
import com.tc.util.State;
import com.tc.util.concurrent.SetOnceFlag;

import java.util.Iterator;

public class StateManagerImpl implements StateManager, GroupMessageListener, GroupEventsListener {

  private static final TCLogger      logger       = TCLogging.getLogger(StateManagerImpl.class);

  private final TCLogger             consoleLogger;
  private final GroupManager         groupManager;
  private final ElectionManager      electionMgr;
  private final Sink                 stateChangeSink;

  private final SetOnceFlag          started      = new SetOnceFlag(false);
  private final CopyOnWriteArrayList listeners    = new CopyOnWriteArrayList();
  private final Object               electionLock = new Object();

  private NodeID                     myNodeId;
  private NodeID                     activeNode   = NodeID.NULL_ID;
  private volatile State             state        = START_STATE;

  public StateManagerImpl(TCLogger consoleLogger, GroupManager groupManager, Sink stateChangeSink) {
    this.consoleLogger = consoleLogger;
    this.groupManager = groupManager;
    this.stateChangeSink = stateChangeSink;
    this.electionMgr = new ElectionManagerImpl(groupManager);
    this.groupManager.registerForMessages(ClusterStateMessage.class, this);
    this.groupManager.registerForGroupEvents(this);
  }

  public void start() {
    started.set();
    try {
      // TODO:: This should be moved out
      this.myNodeId = groupManager.join();
    } catch (GroupException e) {
      logger.error("Caught Exception :", e);
      throw new AssertionError(e);
    }
    info("L2 Node ID = " + myNodeId);
    startElection();
  }

  /*
   * XXX:: If ACTIVE dying before any passive moved to STANDBY state, then the cluster is hung and there is no going
   * around it. If ACTIVE in persistent mode, it can come back and recover the cluster
   */
  private void startElection() {
    synchronized (electionLock) {
      if (state == START_STATE) {
        runElection(true);
      } else if (state == PASSIVE_STANDBY) {
        runElection(false);
      } else {
        info("Ignoring Election request since not in right state");
      }
    }
  }

  private void runElection(boolean isNew) {
    NodeID winner = electionMgr.runElection(myNodeId, isNew);
    if (winner == myNodeId) {
      moveToActiveState();
    }
  }

  public void registerForStateChangeEvents(StateChangeListener listener) {
    listeners.add(listener);
  }

  public void fireStateChangedEvent(StateChangedEvent sce) {
    for (Iterator i = listeners.iterator(); i.hasNext();) {
      StateChangeListener listener = (StateChangeListener) i.next();
      listener.l2StateChanged(sce);
    }
  }

  private synchronized void moveToPassiveState(boolean initialized) {
    electionMgr.reset();
    if (state == START_STATE) {
      state = initialized ? PASSIVE_STANDBY : PASSIVE_UNINTIALIZED;
      info("Moved to " + state, true);
      stateChangeSink.add(new StateChangedEvent(START_STATE, state));
    } else if (state == ACTIVE_COORDINATOR) {
      // TODO:: Support this later
      throw new AssertionError("Cant move to " + PASSIVE_UNINTIALIZED + " from " + ACTIVE_COORDINATOR
                               + " at least for now");
    }
  }
  
  private synchronized void moveToPassiveStandbyState() {
    if(state == ACTIVE_COORDINATOR) {
      // TODO:: Support this later
      throw new AssertionError("Cant move to " + PASSIVE_STANDBY + " from " + ACTIVE_COORDINATOR
                               + " at least for now");
    } else if(state != PASSIVE_STANDBY) {
      stateChangeSink.add(new StateChangedEvent(state, PASSIVE_STANDBY));
      state = PASSIVE_STANDBY;
      info("Moved to " + state, true);
    } else {
     info("Already in " + state);
    }
  }


  private synchronized void moveToActiveState() {
    if (state == START_STATE || state == PASSIVE_STANDBY) {
      // TODO :: If state == START_STATE publish cluster ID
      StateChangedEvent event = new StateChangedEvent(state, ACTIVE_COORDINATOR);
      state = ACTIVE_COORDINATOR;
      this.activeNode = this.myNodeId;
      info("Becoming " + state, true);
      electionMgr.declareWinner(this.myNodeId);
      stateChangeSink.add(event);
    } else {
      throw new AssertionError("Cant move to " + ACTIVE_COORDINATOR + " from " + state);
    }
  }

  public synchronized boolean isActiveCoordinator() {
    return (state == ACTIVE_COORDINATOR);
  }

  public void moveNodeToPassiveStandby(NodeID nodeID) {
    logger.info("Requesting node " + nodeID + " to move to " + PASSIVE_STANDBY);
    GroupMessage msg = ClusterStateMessageFactory.createMoveToPassiveStandbyMessage(EnrollmentFactory
        .createTrumpEnrollment(myNodeId));
    try {
      this.groupManager.sendTo(nodeID, msg);
    } catch (GroupException e) {
      logger.error(e);
    }
  }

  /**
   * Message Listener Interface, TODO::move to a stage
   */
  public synchronized void messageReceived(NodeID fromNode, GroupMessage msg) {
    if (!(msg instanceof ClusterStateMessage)) { throw new AssertionError(
                                                                          "StateManagerImpl : Received wrong message type :"
                                                                              + msg); }
    ClusterStateMessage clusterMsg = (ClusterStateMessage) msg;
    handleClusterStateMessage(clusterMsg);
  }

  private void handleClusterStateMessage(ClusterStateMessage clusterMsg) {
    try {
      switch (clusterMsg.getType()) {
        case ClusterStateMessage.START_ELECTION:
          handleStartElectionRequest(clusterMsg);
          break;
        case ClusterStateMessage.ABORT_ELECTION:
          handleElectionAbort(clusterMsg);
          break;
        case ClusterStateMessage.ELECTION_RESULT:
          handleElectionResultMessage(clusterMsg);
          break;
        case ClusterStateMessage.ELECTION_WON:
          handleElectionWonMessage(clusterMsg);
          break;
        case ClusterStateMessage.MOVE_TO_PASSIVE_STANDBY:
          handleMoveToPassiveStandbyMessage(clusterMsg);
          break;
        default:
          throw new AssertionError("This message shouldn't have been routed here : " + clusterMsg);
      }
    } catch (GroupException ge) {
      logger.error("Caught Exception while handling Message : " + clusterMsg, ge);
      throw new AssertionError(ge);

    }
  }

  private void handleMoveToPassiveStandbyMessage(ClusterStateMessage clusterMsg) {
    moveToPassiveStandbyState();
  }

  private void handleElectionWonMessage(ClusterStateMessage clusterMsg) {
    if (state == ACTIVE_COORDINATOR) {
      // Cant get Election Won from another node : Split brain
      // TODO:: Add some reconcile path
      logger.error(state + " Received Election Won Msg : " + clusterMsg + ". Possible split brain detected ");
      throw new AssertionError(state + " Received Election Won Msg : " + clusterMsg
                               + ". Possible split brain detected ");
    }
    Enrollment winningEnrollment = clusterMsg.getEnrollment();
    this.activeNode = winningEnrollment.getNodeID();
    moveToPassiveState(winningEnrollment.isANewCandidate());
  }

  private void handleElectionResultMessage(ClusterStateMessage msg) throws GroupException {
    if (activeNode.equals(msg.getEnrollment().getNodeID())) {
      Assert.assertFalse(NodeID.NULL_ID.equals(activeNode));
      // This wouldnt normally happen, but we agree - so ack
      GroupMessage resultAgreed = ClusterStateMessageFactory.createResultAgreedMessage(msg, msg.getEnrollment());
      logger.info("Agreed with Election Result from " + msg.messageFrom() + " : " + resultAgreed);
      groupManager.sendTo(msg.messageFrom(), resultAgreed);
    } else if (state == ACTIVE_COORDINATOR || !activeNode.isNull()) {
      // This shouldn't happen normally, but is possible when there is some weird network error where A sees B,
      // B sees A/C and C sees B and A is active and C is trying to run election
      // Force other node to rerun election so that we can abort
      GroupMessage resultConflict = ClusterStateMessageFactory.createResultConflictMessage(msg, EnrollmentFactory
          .createTrumpEnrollment(myNodeId));
      warn("WARNING :: Active Node = " + activeNode + " , " + state
           + " received ELECTION_RESULT message from another node : " + msg + " : Forcing re-election "
           + resultConflict);
      groupManager.sendTo(msg.messageFrom(), resultConflict);
    } else {
      electionMgr.handleElectionResultMessage(msg);
    }
  }

  private void handleElectionAbort(ClusterStateMessage clusterMsg) {
    if (state == ACTIVE_COORDINATOR) {
      // Cant get Abort back to ACTIVE, if so then there is a split brain
      logger.error(state + " Received Abort Election  Msg : Possible split brain detected ");
      throw new AssertionError(state + " Received Abort Election  Msg : Possible split brain detected ");
    }
    electionMgr.handleElectionAbort(clusterMsg);
  }

  private void handleStartElectionRequest(ClusterStateMessage msg) throws GroupException {
    if (state == ACTIVE_COORDINATOR) {
      // This is either a new L2 joining a cluster or a renegade L2. Force it to abort
      GroupMessage abortMsg = ClusterStateMessageFactory.createAbortElectionMessage(msg, EnrollmentFactory
          .createTrumpEnrollment(myNodeId));
      info("Forcing Abort Election for " + msg + " with " + abortMsg);
      groupManager.sendTo(msg.messageFrom(), abortMsg);
    } else {
      electionMgr.handleStartElectionRequest(msg);
    }
  }

  // TODO:: Make it a handler on a stage
  public synchronized void nodeJoined(NodeID nodeID) {
    info("Node : " + nodeID + " joined the cluster", true);
    if (state == ACTIVE_COORDINATOR) {
      // notify new node
      GroupMessage msg = ClusterStateMessageFactory.createElectionWonMessage(EnrollmentFactory
          .createTrumpEnrollment(this.myNodeId));
      try {
        groupManager.sendTo(nodeID, msg);
      } catch (GroupException e) {
        throw new AssertionError(e);
      }
    }
  }

  // TODO:: Make it a handler on a stage
  public void nodeLeft(NodeID nodeID) {
    warn("Node : " + nodeID + " left the cluster", true);
    boolean elect = false;
    synchronized (this) {
      if (state != PASSIVE_UNINTIALIZED && state != ACTIVE_COORDINATOR
          && (activeNode.isNull() || activeNode.equals(nodeID))) {
        elect = true;
        activeNode = NodeID.NULL_ID;
      }
    }
    if (elect) {
      info("Starting Election to determine cluser wide ACTIVE L2");
      startElection();
    }
  }

  private void info(String message) {
    info(message, false);
  }

  private void info(String message, boolean console) {
    logger.info(message);
    if (console) {
      consoleLogger.info(message);
    }
  }

  private void warn(String message) {
    warn(message, false);
  }

  private void warn(String message, boolean console) {
    logger.warn(message);
    if (console) {
      consoleLogger.warn(message);
    }
  }
}
