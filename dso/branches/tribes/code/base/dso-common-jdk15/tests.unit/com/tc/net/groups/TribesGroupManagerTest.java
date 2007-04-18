/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.test.TCTestCase;
import com.tc.util.PortChooser;
import com.tc.util.concurrent.NoExceptionLinkedQueue;
import com.tc.util.concurrent.ThreadUtil;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class TribesGroupManagerTest extends TCTestCase {

  public TribesGroupManagerTest() {
    disableTestUntil("testGroupEventsStatic", "2007-04-27");
  }

  public void testIfTribesGroupManagerLoads() throws Exception {
    GroupManager gm = GroupManagerFactory.createGroupManager();
    assertNotNull(gm);
    assertEquals(TribesGroupManager.class.getName(), gm.getClass().getName());
  }

  public void testGroupEventsMcast() throws Exception {
    final Node[] allNodes = new Node[] { new Node("localhost", 7001), new Node("localhost", 7002) };

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

    assertEquals(n2, gel1.getLastNodeJoined());
    assertEquals(n1, gel2.getLastNodeJoined());

    gm1.stop();
    assertTrue(checkNodeLeftEvent("MCAST", n1, gel2));
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

    assertEquals(n2, gel1.getLastNodeJoined());
    assertEquals(n1, gel2.getLastNodeJoined());

    gm1.stop();
    checkNodeLeftEvent("STATIC", n1, gel2);
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

  private boolean checkNodeLeftEvent(String msg, NodeID n1, MyGroupEventListener gel2) {
    for (int i = 0; i < 10; i++) {
      NodeID actual = gel2.getLastNodeLeft();
      System.err.println("\n### [" + msg + "] attempt # " + i + " -> actualNodeLeft=" + actual);
      if (actual == null) {
        ThreadUtil.reallySleep(1 * 500);
      } else {
        assertEquals(n1, actual);
        System.err.println("\n### [" + msg + "] it took " + (i * 500) + " millis to get NodeLeft event");
        return true;
      }
    }
    return false;
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
