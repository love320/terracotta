/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.exception.ImplementMe;
import com.tc.exception.TCObjectNotFoundException;
import com.tc.net.NodeID;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAException;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.session.SessionID;
import com.tc.util.concurrent.NoExceptionLinkedQueue;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class TestRemoteObjectManager implements RemoteObjectManager {

  public final NoExceptionLinkedQueue retrieveCalls         = new NoExceptionLinkedQueue();
  public final NoExceptionLinkedQueue retrieveResults       = new NoExceptionLinkedQueue();

  public final NoExceptionLinkedQueue retrieveRootIDCalls   = new NoExceptionLinkedQueue();
  public final NoExceptionLinkedQueue retrieveRootIDResults = new NoExceptionLinkedQueue();

  public static final DNA             THROW_NOT_FOUND       = new ThrowNotFound();

  public DNA retrieve(final ObjectID id) {
    this.retrieveCalls.put(id);
    DNA dna = (DNA) this.retrieveResults.take();
    if (dna == THROW_NOT_FOUND) { throw new TCObjectNotFoundException("missing ID", Collections.EMPTY_LIST); }
    return dna;
  }

  public DNA retrieveWithParentContext(final ObjectID id, final ObjectID parentContext) {
    return retrieve(id);
  }

  public ObjectID retrieveRootID(final String name) {
    this.retrieveRootIDCalls.put(name);
    return (ObjectID) this.retrieveRootIDResults.take();
  }

  public void removed(final ObjectID id) {
    // do nothing
  }

  public DNA retrieve(final ObjectID id, final int depth) {
    throw new ImplementMe();
  }

  public void addAllObjects(final SessionID sessionID, final long batchID, final Collection dnas, final NodeID nodeID) {
    throw new ImplementMe();
  }

  public void addRoot(final String name, final ObjectID id, final NodeID nodeID) {
    throw new ImplementMe();
  }

  public void objectsNotFoundFor(final SessionID sessionID, final long batchID, final Set missingObjectIDs,
                                 final NodeID nodeID) {
    throw new ImplementMe();
  }

  public static class ThrowNotFound implements DNA {

    private ThrowNotFound() {
      //
    }

    public int getArraySize() {
      throw new ImplementMe();
    }

    public DNACursor getCursor() {
      throw new ImplementMe();
    }

    public String getDefiningLoaderDescription() {
      throw new ImplementMe();
    }

    public ObjectID getObjectID() throws DNAException {
      throw new ImplementMe();
    }

    public ObjectID getParentObjectID() throws DNAException {
      throw new ImplementMe();
    }

    public String getTypeName() {
      throw new ImplementMe();
    }

    public long getVersion() {
      throw new ImplementMe();
    }

    public boolean hasLength() {
      throw new ImplementMe();
    }

    public boolean isDelta() {
      throw new ImplementMe();
    }
  }

  public void clear() {
    throw new ImplementMe();
  }

  public boolean isPrefetched(final ObjectID id) {
    throw new ImplementMe();
  }

  public void initializeHandshake(final NodeID thisNode, final NodeID remoteNode,
                                  final ClientHandshakeMessage handshakeMessage) {
    throw new ImplementMe();

  }

  public void pause(final NodeID remoteNode, final int disconnected) {
    throw new ImplementMe();

  }

  public void unpause(final NodeID remoteNode, final int disconnected) {
    throw new ImplementMe();

  }

  public void preFetchObject(ObjectID id) {
    throw new ImplementMe();
  }
}
