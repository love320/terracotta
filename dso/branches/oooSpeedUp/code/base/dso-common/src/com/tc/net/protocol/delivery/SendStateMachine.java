/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.delivery;

import EDU.oswego.cs.dl.util.concurrent.BoundedLinkedQueue;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedInt;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.properties.ReconnectConfig;
import com.tc.util.Assert;
import com.tc.util.DebugUtil;

import java.util.LinkedList;
import java.util.ListIterator;

/**
 * 
 */
public class SendStateMachine extends AbstractStateMachine {
  private final int                        sendQueueCap;
  private final State                      ACK_WAIT_STATE       = new AckWaitState();
  private final State                      HANDSHAKE_WAIT_STATE = new HandshakeWaitState();
  private final State                      MESSAGE_WAIT_STATE   = new MessageWaitState();
  private long                             sent                 = -1;
  private long                             acked                = -1;
  private final OOOProtocolMessageDelivery delivery;
  private BoundedLinkedQueue               sendQueue;
  private final LinkedList                 outstandingMsgs      = new LinkedList();
  private final SynchronizedInt            outstandingCnt       = new SynchronizedInt(0);
  private final int                        sendWindow;
  private final boolean                    isClient;
  private final String                     debugId;
  private static final boolean             debug                = false;
  private static final TCLogger            logger               = TCLogging.getLogger(SendStateMachine.class);

  // changed by tc.properties

  public SendStateMachine(OOOProtocolMessageDelivery delivery, ReconnectConfig reconnectConfig, boolean isClient) {
    super();

    this.delivery = delivery;
    // set sendWindow from tc.properties if exist. 0 to disable window send.
    sendWindow = reconnectConfig.getSendWindow();
    int queueCap = reconnectConfig.getSendQueueCapacity();
    this.sendQueueCap = (queueCap == 0) ? Integer.MAX_VALUE : queueCap;
    this.sendQueue = new BoundedLinkedQueue(this.sendQueueCap);
    this.isClient = isClient;
    this.debugId = (this.isClient) ? "CLIENT" : "SERVER";
  }

  protected void basicResume() {
    switchToState(HANDSHAKE_WAIT_STATE);
  }

  protected State initialState() {
    Assert.eval(MESSAGE_WAIT_STATE != null);
    return MESSAGE_WAIT_STATE;
  }

  public synchronized void execute(OOOProtocolMessage msg) {
    Assert.eval(isStarted());
    getCurrentState().execute(msg);
  }

  protected synchronized void switchToState(State state) {
    debugLog("switching to " + state);
    super.switchToState(state);
  }

  private class MessageWaitState extends AbstractState {

    public MessageWaitState() {
      super("MESSAGE_WAIT_STATE");
    }

    public void enter() {
      //
    }

    public void execute(OOOProtocolMessage protocolMessage) {
      if ((protocolMessage != null) && protocolMessage.isAck()) {
        switchToState(ACK_WAIT_STATE);
        getCurrentState().execute(protocolMessage);
      } else {
        // instead of waiting for every protocol event to send message, try at one shot
        sendMoreIfAvailable();
        if ((sendWindow != 0) && (outstandingCnt.get() >= sendWindow)) {
          switchToState(ACK_WAIT_STATE);
        }
      }
    }
  }

  private class HandshakeWaitState extends AbstractState {

    public HandshakeWaitState() {
      super("HANDSHAKE_WAIT_STATE");
    }

    public void execute(OOOProtocolMessage msg) {
      if (msg == null) return;
      // drop all msgs until handshake reply.
      // Happens when short network disruptions and both L1 & L2 still keep states.
      if (!msg.isHandshakeReplyOk() && !msg.isHandshakeReplyFail()) {
        logger.warn("Due to handshake drops stale message:" + msg);
        return;
      }

      if (msg.isHandshakeReplyFail()) {
        switchToState(MESSAGE_WAIT_STATE);
        return;
      }

      long ackedSeq = msg.getAckSequence();

      if (ackedSeq == -1) {
        debugLog("The other side restarted [switching to MSG_WAIT_STATE]");
        switchToState(MESSAGE_WAIT_STATE);
        return;
      }
      if (ackedSeq < acked) {
        // this shall not, old ack
        Assert.failure("Received bad ack: " + ackedSeq + " expected >= " + acked);
      } else {
        logger.info("SENDER-" + debugId + "-" + delivery.getConnectionId() + "; AckSeq: " + ackedSeq + " Acked: "
                    + acked);

        while (ackedSeq > acked) {
          ++acked;
          removeMessage();
        }
        // resend outstanding which is not acked
        if (outstandingCnt.get() > 0) {
          // resend those not acked
          resendOutstandings();
          switchToState(ACK_WAIT_STATE);
        } else {
          // all acked, we're good here
          switchToState(MESSAGE_WAIT_STATE);
        }
      }
    }
  }

  private class AckWaitState extends AbstractState {

    public AckWaitState() {
      super("ACK_WAIT_STATE");
    }

    public void enter() {
      //
    }

    public void execute(OOOProtocolMessage protocolMessage) {
      if (protocolMessage == null || protocolMessage.isSend()) return;

      long ackedSeq = protocolMessage.getAckSequence();
      Assert.eval("SENDER-" + debugId + "-" + delivery.getConnectionId() + ": AckSeq " + ackedSeq
                  + " should be greater than " + acked, ackedSeq >= acked);

      while (ackedSeq > acked) {
        ++acked;
        removeMessage();
      }
      Assert.eval(acked <= sent);

      switchToState(MESSAGE_WAIT_STATE);
      sendMoreIfAvailable();
    }

  }

  private void sendMoreIfAvailable() {
    while (((sendWindow == 0) || (outstandingCnt.get() < sendWindow)) && !sendQueue.isEmpty()) {
      delivery.sendMessage(createProtocolMessage(++sent));
    }
  }

  private OOOProtocolMessage createProtocolMessage(long count) {
    final OOOProtocolMessage opm = delivery.createProtocolMessage(count, dequeue(sendQueue));
    Assert.eval(opm != null);
    outstandingCnt.increment();
    outstandingMsgs.add(opm);
    return (opm);
  }

  private void resendOutstandings() {
    ListIterator it = outstandingMsgs.listIterator(0);
    while (it.hasNext()) {
      OOOProtocolMessage msg = (OOOProtocolMessage) it.next();
      delivery.sendMessage(msg);
    }
  }

  private void removeMessage() {
    OOOProtocolMessage msg = (OOOProtocolMessage) outstandingMsgs.removeFirst();
    msg.reallyDoRecycleOnWrite();
    outstandingCnt.decrement();
    Assert.eval(outstandingCnt.get() >= 0);
  }

  public synchronized void reset() {

    sent = -1;
    acked = -1;

    // purge out outstanding sends
    outstandingCnt.set(0);
    outstandingMsgs.clear();

    BoundedLinkedQueue tmpQ = sendQueue;
    sendQueue = new BoundedLinkedQueue(sendQueueCap);
    while (!tmpQ.isEmpty()) {
      dequeue(tmpQ);
    }
  }

  private static TCNetworkMessage dequeue(BoundedLinkedQueue q) {
    try {
      return (TCNetworkMessage) q.take();
    } catch (InterruptedException e) {
      throw new AssertionError(e);
    }
  }

  public void put(TCNetworkMessage message) throws InterruptedException {
    sendQueue.put(message);
  }

  private void debugLog(String msg) {
    if (debug) {
      DebugUtil.trace("SENDER-" + debugId + "-" + delivery.getConnectionId() + " -> " + msg);
    }
  }

  // for testing purpose only
  boolean isClean() {
    return (sendQueue.isEmpty() && outstandingMsgs.isEmpty());
  }

}
