/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import org.apache.commons.io.FileUtils;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedInt;

import com.tc.cluster.ClusterEventListener;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.objectserver.control.ExtraL1ProcessControl;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;

import java.io.File;
import java.util.HashSet;

public class ClusterMembershipEventTestApp extends ServerCrashingAppBase implements ClusterEventListener {

  public ClusterMembershipEventTestApp(String appId, ApplicationConfig config, ListenerProvider listenerProvider) {
    super(appId, config, listenerProvider);
  }

  private final int             initialNodeCount = getParticipantCount();
  private final CyclicBarrier   barrier          = new CyclicBarrier(initialNodeCount);

  // not shared..
  private final SynchronizedInt nodeConCnt       = new SynchronizedInt(0);
  private final SynchronizedInt nodeDisCnt       = new SynchronizedInt(0);
  private final SynchronizedInt thisNodeConCnt   = new SynchronizedInt(0);
  private final SynchronizedInt thisNodeDisCnt   = new SynchronizedInt(0);
  private final HashSet         nodes            = new HashSet();
  private String                thisNode;

  public void run() {
    try {
      runTest();
    } catch (Throwable t) {
      notifyError(t);
    }
  }

  private void runTest() throws Throwable {
    ManagerUtil.addClusterEventListener(this);
    check(1, thisNodeConCnt.get(), "thisNodeConnected");
    waitForNodes(initialNodeCount);

    System.err.println("### stage 1 [all nodes connected]: thisNode=" + thisNode + ", threadId="
                       + Thread.currentThread().getName());

    clearCounters();
    final boolean isMasterNode = barrier.barrier() == 0;
    if (isMasterNode) {
      System.err.println("### masterNode=" + thisNode + " -> crashing server...");
      getConfig().getServerControl().crash();
      System.err.println("### masterNode=" + thisNode + " -> crashed server");
      System.err.println("### masterNode=" + thisNode + " -> restarting server...");
      getConfig().getServerControl().start(30 * 1000);
      System.err.println("### masterNode=" + thisNode + " -> restarted server");
    }
    System.err.println("### stage 2 [reconnecting]: thisNode=" + thisNode + ", threadId="
                       + Thread.currentThread().getName());
    barrier.barrier();
    waitForNodes(initialNodeCount);
    check(1, thisNodeDisCnt.get(), "thisNodeDisconnected");
    check(1, thisNodeConCnt.get(), "thisNodeConnected");

    clearCounters();
    check(0, nodeConCnt.get(), "nodeConnected");
    check(0, nodeDisCnt.get(), "nodeDisconnected");
    barrier.barrier();
    System.err.println("### stage 3 [reconnected]: thisNode=" + thisNode + ", threadId="
                       + Thread.currentThread().getName());

    if (isMasterNode) {
      // master node blocks until new client exists...
      spawnNewClient();
    }
    barrier.barrier();
    System.err.println("### stage 4 [new client disconnected]: thisNode=" + thisNode + ", threadId="
                       + Thread.currentThread().getName());

    waitForNodes(initialNodeCount);
    check(1, nodeConCnt.get(), "nodeConnected");
    check(1, nodeDisCnt.get(), "nodeDisconnected");
    clearCounters();
    barrier.barrier();
    System.err.println("### stage 5 [all done]: thisNode=" + thisNode + ", threadId="
                       + Thread.currentThread().getName());
  }

  private void clearCounters() {
    this.nodeConCnt.set(0);
    this.nodeDisCnt.set(0);
    this.thisNodeConCnt.set(0);
    this.thisNodeDisCnt.set(0);
  }

  private void waitForNodes(int expectedSize) {
    while (true) {
      synchronized (nodes) {
        if (nodes.size() == expectedSize) break;
        try {
          nodes.wait();
        } catch (InterruptedException e) {
          notifyError(e);
        }
      }
    }
  }

  private void check(int expected, int actual, String msg) {
    if (expected != actual) notifyError(msg + " expected=" + expected + ", actual=" + actual + ", thisNodeId="
                                        + thisNode);
  }

  public void nodeConnected(String nodeId) {
    new Throwable("### TRACE: ClusterMembershipEventTestApp.nodeConnected()").printStackTrace();
    nodeConCnt.increment();
    System.err.println("\n### nodeConnected: thisNode=" + thisNode + ", nodeId=" + nodeId + ", threadId="
                       + Thread.currentThread().getName() + ", cnt=" + nodeConCnt.get());
    synchronized (nodes) {
      nodes.add(nodeId);
      nodes.notifyAll();
    }
  }

  public void nodeDisconnected(String nodeId) {
    new Throwable("### TRACE: ClusterMembershipEventTestApp.nodeDisconnected()").printStackTrace();
    nodeDisCnt.increment();
    System.err.println("\n### nodeDisconnected: thisNode=" + thisNode + ", nodeId=" + nodeId + ", threadId="
                       + Thread.currentThread().getName() + ", cnt=" + nodeDisCnt.get());
    synchronized (nodes) {
      nodes.remove(nodeId);
      nodes.notifyAll();
    }
  }

  public void thisNodeConnected(String thisNodeId, String[] nodesCurrentlyInCluster) {
    new Throwable("### TRACE: ClusterMembershipEventTestApp.thisNodeConnected()").printStackTrace();
    thisNodeConCnt.increment();
    thisNode = thisNodeId;
    System.err.println("\n### thisNodeConnected->thisNodeId=" + thisNodeId + ", threadId="
                       + Thread.currentThread().getName() + ", cnt=" + thisNodeConCnt.get());
    synchronized (nodes) {
      nodes.add(thisNode);
      for (int i = 0; i < nodesCurrentlyInCluster.length; i++) {
        nodes.add(nodesCurrentlyInCluster[i]);
      }
      nodes.notifyAll();
    }
  }

  public void thisNodeDisconnected(String thisNodeId) {
    new Throwable("### TRACE: ClusterMembershipEventTestApp.thisNodeDisconnected()").printStackTrace();
    thisNodeDisCnt.increment();
    System.err.println("\n### thisNodeDisconnected->thisNodeId=" + thisNodeId + ", threadId="
                       + Thread.currentThread().getName() + ", cnt=" + thisNodeDisCnt.get());
    synchronized (nodes) {
      nodes.clear();
      nodes.notifyAll();
    }
  }

  public static class L1Client {
    public static void main(String args[]) {
      // nothing to do
    }
  }

  private ExtraL1ProcessControl spawnNewClient() throws Exception {
    final String hostName = getHostName();
    final int port = getPort();
    final File configFile = new File(getConfigFilePath());
    File workingDir = new File(configFile.getParentFile(), "client-0");
    FileUtils.forceMkdir(workingDir);

    ExtraL1ProcessControl client = new ExtraL1ProcessControl(hostName, port, L1Client.class, configFile
        .getAbsolutePath(), new String[0], workingDir);
    client.start(20000);
    client.mergeSTDERR();
    client.mergeSTDOUT();
    client.waitFor();
    System.err.println("\n### Started New Client");
    return client;
  }

}
