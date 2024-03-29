/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.l1.api;

import com.tc.exception.ImplementMe;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.ObjectID;
import com.tc.objectserver.managedobject.BackReferences;
import com.tc.text.PrettyPrinter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class TestClientStateManager implements ClientStateManager {

  public ChannelID  shutdownClient    = null;
  public Collection allClientIDs      = new HashSet();
  public List       addReferenceCalls = new ArrayList();

  public void shutdownClient(ChannelID deadClient) {
    this.shutdownClient = deadClient;
  }

  public void addReference(ChannelID clientID, ObjectID objectID) {
    addReferenceCalls.add(new AddReferenceContext(clientID, objectID));
  }

  public static class AddReferenceContext {
    public final ChannelID clientID;
    public final ObjectID  objectID;

    private AddReferenceContext(ChannelID clientID, ObjectID objectID) {
      this.clientID = clientID;
      this.objectID = objectID;
    }
  }

  public void removeReferences(ChannelID clientID, Set removed) {
    //
  }

  public List createPrunedChangesAndAddObjectIDTo(Collection changes, BackReferences includeIDs, ChannelID clientID,
                                                  Set objectIDs) {
    return Collections.EMPTY_LIST;
  }

  public boolean hasReference(ChannelID clientID, ObjectID objectID) {
    // to be consistent with createPrunedChangesAndAddObjectIDTo, return false
    return false;
  }

  public void addAllReferencedIdsTo(Set rescueIds) {
    throw new ImplementMe();

  }

  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    return out.print(getClass().getName());
  }

  public Collection getAllClientIDs() {
    return allClientIDs;
  }

  public void stop() {
    // TODO Auto-generated method stub

  }

  public void removeReferencedFrom(ChannelID channelID, Set secondPass) {
    throw new ImplementMe();

  }

  public Set addReferences(ChannelID channelID, Set oids) {
    for (Iterator i = oids.iterator(); i.hasNext();) {
      ObjectID oid = (ObjectID) i.next();
      addReferenceCalls.add(new AddReferenceContext(channelID, oid));
    }
    return oids;
  }

  public void startupClient(ChannelID channelID) {
    // NOP
  }

}