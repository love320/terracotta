/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.async.api.EventContext;
import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutput;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.ObjectID;
import com.tc.object.session.SessionID;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class ObjectsNotFoundMessage extends DSOMessageBase implements EventContext {

  private final static byte BATCH_ID    = 0;
  private final static byte MISSING_OID = 1;

  private Set               missingOids;
  private long              batchID;

  public ObjectsNotFoundMessage(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutput out,
                                MessageChannel channel, TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public ObjectsNotFoundMessage(SessionID sessionID, MessageMonitor monitor, MessageChannel channel,
                                TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  public void initialize(Set missingObjectIDs, long batchId) {
    this.missingOids = missingObjectIDs;
    this.batchID = batchId;
  }

  protected void dehydrateValues() {
    putNVPair(BATCH_ID, batchID);
    for (Iterator i = missingOids.iterator(); i.hasNext();) {
      ObjectID oid = (ObjectID) i.next();
      putNVPair(MISSING_OID, oid.toLong());
    }
  }

  protected boolean hydrateValue(byte name) throws IOException {
    switch (name) {
      case BATCH_ID:
        this.batchID = getLongValue();
        return true;
      case MISSING_OID:
        if (missingOids == null) {
          missingOids = new HashSet();
        }
        this.missingOids.add(new ObjectID(getLongValue()));
        return true;
      default:
        return false;
    }
  }

  public long getBatchID() {
    return batchID;
  }

  public Set getMissingObjectIDs() {
    return missingOids;
  }

}
