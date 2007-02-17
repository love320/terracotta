/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import java.util.Collections;
import java.util.List;

/*
 * This is a simple class that is a dummy group manager. All it does is it treats this one node that it runs in as a
 * group. This is needed for two reasons, 1) to easly disable, test the rest of the system and 2) to provide an
 * interface level replacement for tribes in 1.4 JVM
 */
public class SingleNodeGroupManager implements GroupManager {

  private static final GroupResponse DUMMY_RESPONSE  = new GroupResponse() {
                                                       public List getResponses() {
                                                         return Collections.EMPTY_LIST;
                                                       }
                                                     };

  private static final byte[]        CURRENT_NODE_ID = new byte[] { 36, 24, 32 };

  NodeID                             thisNode;

  public NodeID join() throws GroupException {
    if (thisNode != null) { throw new GroupException("Already Joined"); }
    this.thisNode = new NodeID("CurrentNode", CURRENT_NODE_ID);
    return this.thisNode;
  }

  public void registerForMessages(Class msgClass, GroupMessageListener listener) {
    // NOP : Since this doesnt talk to the network, this should never get any message
  }

  public void sendAll(GroupMessage msg) {
    // NOP : No Network, no one to write to
  }

  public GroupResponse sendAllAndWaitForResponse(GroupMessage msg) {
    // NOP : No Network, no one to write to, hen no response too
    return DUMMY_RESPONSE;
  }

  public void sendTo(NodeID node, GroupMessage msg) throws GroupException {
    throw new GroupException("Can't write to Node : " + node + " Node Not found !");
  }

  public void registerForGroupEvents(GroupEventsListener listener) {
    // NOP : No network, no one joins or leaves
  }

}
