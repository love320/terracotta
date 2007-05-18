/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandler;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.test.TCTestCase;
import com.tc.util.PortChooser;
import com.tc.util.concurrent.NoExceptionLinkedQueue;
import com.tc.util.concurrent.ThreadUtil;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class TribesGroupManagerTest extends TCTestCase {

  private static final TCLogger logger = TCLogging.getLogger(TribesGroupManager.class);

  public void testIfTribesGroupManagerLoads() throws Exception {
    GroupManager gm = GroupManagerFactory.createGroupManager();
    assertNotNull(gm);
    assertEquals(TribesGroupManager.class.getName(), gm.getClass().getName());
  }

  public void testGroupEventsMcast() throws Exception {
    TribesGroupManager gm1 = new TribesGroupManager();
    MyGroupEventListener gel1 = new MyGroupEventListener();
    MyListener l1 = new MyListener();
    gm1.registerForMessages(TestMessage.class, l1);
    gm1.registerForGroupEvents(gel1);
    NodeID n1 = gm1.joinMcast();

    TribesGroupManager gm2 = new TribesGroupManager();
    MyGroupEventListener gel2 = new MyGroupEventListener();
    MyListener l2 = new MyListener();
    gm2.registerForMessages(TestMessage.class, l2);
    gm2.registerForGroupEvents(gel2);
    NodeID n2 = gm2.joinMcast();

    assertTrue(checkGroupEvent("MCAST", n2, gel1, true));
    assertTrue(checkGroupEvent("MCAST", n1, gel2, true));

    gm1.stop();
    assertTrue(checkGroupEvent("MCAST", n1, gel2, false));
    gm2.stop();
  }

  public void testGroupEventsStatic() throws Exception {
    PortChooser pc = new PortChooser();
    final int p1 = pc.chooseRandomPort();
    final int p2 = pc.chooseRandomPort();
    final Node[] allNodes = new Node[] { new Node("localhost", p1), new Node("localhost", p2) };

    TribesGroupManager gm1 = new TribesGroupManager();
    MyGroupEventListener gel1 = new MyGroupEventListener();
    MyListener l1 = new MyListener();
    gm1.registerForMessages(TestMessage.class, l1);
    gm1.registerForGroupEvents(gel1);
    NodeID n1 = gm1.joinStatic(allNodes[0], allNodes);

    TribesGroupManager gm2 = new TribesGroupManager();
    MyListener l2 = new MyListener();
    MyGroupEventListener gel2 = new MyGroupEventListener();
    gm2.registerForMessages(TestMessage.class, l2);
    gm2.registerForGroupEvents(gel2);
    NodeID n2 = gm2.joinStatic(allNodes[1], allNodes);
    assertNotEquals(n1, n2);

    assertTrue(checkGroupEvent("STATIC", n2, gel1, true));
    assertTrue(checkGroupEvent("STATIC", n1, gel2, true));

    gm1.stop();
    assertTrue(checkGroupEvent("STATIC", n1, gel2, false));
    gm2.stop();
  }

  public void testSendingReceivingMessagesMcast() throws Exception {
    TribesGroupManager gm1 = new TribesGroupManager();
    MyListener l1 = new MyListener();
    gm1.registerForMessages(TestMessage.class, l1);
    NodeID n1 = gm1.joinMcast();

    TribesGroupManager gm2 = new TribesGroupManager();
    MyListener l2 = new MyListener();
    gm2.registerForMessages(TestMessage.class, l2);
    NodeID n2 = gm2.joinMcast();
    assertNotEquals(n1, n2);
    checkSendingReceivingMessages(gm1, l1, gm2, l2);
    gm1.stop();
    gm2.stop();
  }

  public void testSendingReceivingMessagesStatic() throws Exception {
    PortChooser pc = new PortChooser();
    final int p1 = pc.chooseRandomPort();
    final int p2 = pc.chooseRandomPort();
    final Node[] allNodes = new Node[] { new Node("localhost", p1), new Node("localhost", p2) };

    TribesGroupManager gm1 = new TribesGroupManager();
    MyListener l1 = new MyListener();
    gm1.registerForMessages(TestMessage.class, l1);
    NodeID n1 = gm1.joinStatic(allNodes[0], allNodes);

    TribesGroupManager gm2 = new TribesGroupManager();
    MyListener l2 = new MyListener();
    gm2.registerForMessages(TestMessage.class, l2);
    NodeID n2 = gm2.joinStatic(allNodes[1], allNodes);
    assertNotEquals(n1, n2);
    checkSendingReceivingMessages(gm1, l1, gm2, l2);
    gm1.stop();
    gm2.stop();
  }

  private void checkSendingReceivingMessages(TribesGroupManager gm1, MyListener l1, TribesGroupManager gm2,
                                             MyListener l2) throws GroupException {
    ThreadUtil.reallySleep(5 * 1000);

    TestMessage m1 = new TestMessage("Hello there");
    gm1.sendAll(m1);

    TestMessage m2 = (TestMessage) l2.take();
    System.err.println(m2);

    assertEquals(m1, m2);

    TestMessage m3 = new TestMessage("Hello back");
    gm2.sendAll(m3);

    TestMessage m4 = (TestMessage) l1.take();
    System.err.println(m4);

    assertEquals(m3, m4);
  }

  public void testMessagesOrderingStatic() throws Exception {
    PortChooser pc = new PortChooser();
    final int p1 = pc.chooseRandomPort();
    final int p2 = pc.chooseRandomPort();
    final Node[] allNodes = new Node[] { new Node("localhost", p1), new Node("localhost", p2) };
    
    System.err.println("Testing message Ordering - Static");

    TribesGroupManager gm1 = new TribesGroupManager();
    NodeID n1 = gm1.joinStatic(allNodes[0], allNodes);
    TribesGroupManager gm2 = new TribesGroupManager();
    NodeID n2 = gm2.joinStatic(allNodes[1], allNodes);

    assertNotEquals(n1, n2);

    checkMessagesOrdering(gm1, gm2);

    gm1.stop();
    gm2.stop();
  }
  
  public void testMessagesOrderingMcast() throws Exception {
    System.err.println("Testing message Ordering - Mcast");
    TribesGroupManager gm1 = new TribesGroupManager();
    NodeID n1 = gm1.joinMcast();
    TribesGroupManager gm2 = new TribesGroupManager();
    NodeID n2 = gm2.joinMcast();

    assertNotEquals(n1, n2);
    
    checkMessagesOrdering(gm1, gm2);

    gm1.stop();
    gm2.stop();
  }

  private void checkMessagesOrdering(final TribesGroupManager mgr1, final TribesGroupManager mgr2)
      throws GroupException {

    final Integer upbound = new Integer(50);
    final MyListener myl1 = new MyListener();
    final MyListener myl2 = new MyListener();
    mgr1.registerForMessages(TestMessage.class, myl1);
    mgr2.registerForMessages(TestMessage.class, myl2);

    // setup throwable ThreadGroup to catch AssertError from threads.
    TCThreadGroup threadGroup = new TCThreadGroup(new ThrowableHandler(logger), "StateManagerTestGroup");
    ThreadUtil.reallySleep(1000);

    Thread t1 = new SenderThread(threadGroup, "Node-0", mgr1, upbound);
    Thread t2 = new SenderThread(threadGroup, "Node-1", mgr2, upbound);
    Thread vt1 = new ReceiverThread(threadGroup, "Node-0", myl1, upbound);
    Thread vt2 = new ReceiverThread(threadGroup, "Node-1", myl2, upbound);

    System.err.println("*** Start sending ordered messages....");
    t1.start();
    t2.start();
    vt1.start();
    vt2.start();

    try {
      t1.join();
      t2.join();
      vt1.join();
      vt2.join();
    } catch (InterruptedException x) {
      throw new GroupException("Join interrupted:" + x);
    }
    System.err.println("*** Done with messages ordering test");

  }

  private boolean checkGroupEvent(String msg, NodeID n1, MyGroupEventListener gel2, boolean checkNodeJoined) {
    final String event = (checkNodeJoined) ? "NodeJoined" : "NodeLeft";
    for (int i = 0; i < 10; i++) {
      NodeID actual = null;
      if (checkNodeJoined) actual = gel2.getLastNodeJoined();
      else actual = gel2.getLastNodeLeft();
      System.err.println("\n### [" + msg + "] attempt # " + i + " -> actual" + event + "=" + actual);
      if (actual == null) {
        ThreadUtil.reallySleep(1 * 500);
      } else {
        assertTrue(n1.equals(actual) || n1.getName().equals(actual.getName()));
        System.err.println("\n### [" + msg + "] it took " + (i * 500) + " millis to get " + event + " event");
        return true;
      }
    }
    return false;
  }

  private static final class SenderThread extends Thread {
    TribesGroupManager mgr;
    Integer            upbound;
    Integer            index = new Integer(0);

    public SenderThread(ThreadGroup group, String name, TribesGroupManager mgr, Integer upbound) {
      super(group, name);
      this.mgr = mgr;
      this.upbound = upbound;
    }

    public void run() {
      while (index <= upbound) {
        TestMessage msg = new TestMessage(index.toString());
        System.err.println("*** " + getName() + " sends " + index);
        try {
          mgr.sendAll(msg);
        } catch (Exception x) {
          throw new RuntimeException("sendAll GroupException:" + x);
        }
        ++index;
      }
    }
  }

  private static final class ReceiverThread extends Thread {
    MyListener l;
    Integer    upbound;
    Integer    index = new Integer(0);

    public ReceiverThread(ThreadGroup group, String name, MyListener l, Integer upbound) {
      super(group, name);
      this.l = l;
      this.upbound = upbound;
    }

    public void run() {
      while (index < upbound) {
        TestMessage msg = (TestMessage) l.take();
        System.err.println("*** " + getName() + " receives " + msg);
        assertEquals(new TestMessage(index.toString()), msg);
        index++;
      }
    }

  }

  private static final class MyGroupEventListener implements GroupEventsListener {

    private NodeID lastNodeJoined;
    private NodeID lastNodeLeft;

    public void nodeJoined(NodeID nodeID) {
      System.err.println("\n### nodeJoined -> " + nodeID.getName());
      lastNodeJoined = nodeID;
    }

    public void nodeLeft(NodeID nodeID) {
      System.err.println("\n### nodeLeft -> " + nodeID.getName());
      lastNodeLeft = nodeID;
    }

    public NodeID getLastNodeJoined() {
      return lastNodeJoined;
    }

    public NodeID getLastNodeLeft() {
      return lastNodeLeft;
    }

    public void reset() {
      lastNodeJoined = lastNodeLeft = null;
    }
  }

  private static final class MyListener implements GroupMessageListener {

    NoExceptionLinkedQueue queue = new NoExceptionLinkedQueue();

    public void messageReceived(NodeID fromNode, GroupMessage msg) {
      queue.put(msg);
    }

    public GroupMessage take() {
      return (GroupMessage) queue.take();
    }

  }

  private static final class TestMessage extends AbstractGroupMessage {

    // to make serialization sane
    public TestMessage() {
      super(0);
    }

    public TestMessage(String message) {
      super(0);
      this.msg = message;
    }

    String msg;

    @Override
    protected void basicReadExternal(int msgType, ObjectInput in) throws IOException {
      msg = in.readUTF();

    }

    @Override
    protected void basicWriteExternal(int msgType, ObjectOutput out) throws IOException {
      out.writeUTF(msg);

    }

    public int hashCode() {
      return msg.hashCode();
    }

    public boolean equals(Object o) {
      if (o instanceof TestMessage) {
        TestMessage other = (TestMessage) o;
        return this.msg.equals(other.msg);
      }
      return false;
    }

    public String toString() {
      return "TestMessage [ " + msg + "]";
    }
  }
}
