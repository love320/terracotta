/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol.tcm;

import com.tc.async.api.Sink;
import com.tc.config.schema.dynamic.ConfigItem;
import com.tc.exception.ImplementMe;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.TCConnectionManager;
import com.tc.net.protocol.transport.ConnectionIdFactory;
import com.tc.object.session.SessionProvider;

import java.util.Set;

public class TestCommunicationsManager implements CommunicationsManager {

  public boolean shutdown;

  public TCConnectionManager getConnectionManager() {
    throw new ImplementMe();
  }

  public void shutdown() {
    throw new ImplementMe();
  }

  public NetworkListener[] getAllListeners() {
    throw new ImplementMe();
  }

  public ClientMessageChannel createClientChannel(SessionProvider sessionProvider, int maxReconnectTries,
                                                  String hostname, int port, int timeout,
                                                  ConfigItem connectionInfoSource) {
    throw new ImplementMe();
  }

  public NetworkListener createListener(SessionProvider sessionProvider, TCSocketAddress addr,
                                        boolean transportDisconnectRemovesChannel, Set initialConnectionIDs,
                                        ConnectionIdFactory connectionIdFactory) {
    throw new ImplementMe();
  }

  public NetworkListener createListener(SessionProvider sessionProvider, TCSocketAddress addr,
                                        boolean transportDisconnectRemovesChannel, Set initialConnectionIDs,
                                        ConnectionIdFactory connectionIdFactory, boolean reuseAddress) {
    throw new ImplementMe();
  }

  public boolean isInShutdown() {
    return this.shutdown;
  }

  public NetworkListener createListener(SessionProvider sessionProvider, TCSocketAddress address, boolean b,
                                        Set initialConnectionIDs, ConnectionIdFactory connectionIDFactory, Sink httpSink) {
    throw new ImplementMe();
  }

}