/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.io.TCSerializable;
import com.tc.net.NodeID;
import com.tc.net.groups.NodeIDSerializer;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.ObjectID;
import com.tc.object.dmi.DmiDescriptor;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.impl.DNAImpl;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.dna.impl.VersionizedDNAWrapper;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.lockmanager.api.LockContext;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.session.SessionID;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.TxnType;
import com.tc.util.Assert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author steve
 */
public class BroadcastTransactionMessageImpl extends DSOMessageBase implements BroadcastTransactionMessage {
  private final static byte      DNA_ID                = 1;
  private final static byte      LOCK_ID               = 2;
  private final static byte      CHANGE_ID             = 3;
  private final static byte      TRANSACTION_ID        = 4;
  private final static byte      COMMITTER_ID          = 5;
  private final static byte      TRANSACTION_TYPE_ID   = 6;
  private final static byte      GLOBAL_TRANSACTION_ID = 7;
  private final static byte      LOW_WATERMARK         = 8;
  private final static byte      SERIALIZER_ID         = 9;
  private final static byte      NOTIFIED              = 10;
  private final static byte      ROOT_NAME_ID_PAIR     = 11;
  private final static byte      DMI_ID                = 12;

  private List                   changes               = new LinkedList();
  private final List             dmis                  = new LinkedList();
  private final Collection       notifies              = new LinkedList();
  private final Map              newRoots              = new HashMap();
  private List                   lockIDs;

  private long                   changeID;
  private TransactionID          transactionID;
  private NodeID                 committerID;
  private TxnType                transactionType;
  private GlobalTransactionID    globalTransactionID;
  private GlobalTransactionID    lowWatermark;
  private ObjectStringSerializer serializer;

  public BroadcastTransactionMessageImpl(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutputStream out,
                                         MessageChannel channel, TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public BroadcastTransactionMessageImpl(SessionID sessionID, MessageMonitor monitor, MessageChannel channel,
                                         TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  @Override
  protected void dehydrateValues() {
    putNVPair(TRANSACTION_TYPE_ID, this.transactionType.getType());
    for (Iterator i = this.lockIDs.iterator(); i.hasNext();) {
      LockID lockID = (LockID) i.next();
      putNVPair(LOCK_ID, lockID.asString());
    }

    for (Iterator i = this.notifies.iterator(); i.hasNext();) {
      LockContext notified = (LockContext) i.next();
      putNVPair(NOTIFIED, notified);
    }

    putNVPair(SERIALIZER_ID, this.serializer);

    putNVPair(CHANGE_ID, this.changeID);
    putNVPair(TRANSACTION_ID, this.transactionID.toLong());
    putNVPair(COMMITTER_ID, new NodeIDSerializer(this.committerID));
    putNVPair(GLOBAL_TRANSACTION_ID, this.globalTransactionID.toLong());
    putNVPair(LOW_WATERMARK, this.lowWatermark.toLong());

    for (Iterator i = this.changes.iterator(); i.hasNext();) {
      DNAImpl dna = (DNAImpl) i.next();
      putNVPair(DNA_ID, dna);
    }
    for (Iterator i = this.newRoots.keySet().iterator(); i.hasNext();) {
      String key = (String) i.next();
      ObjectID value = (ObjectID) this.newRoots.get(key);
      putNVPair(ROOT_NAME_ID_PAIR, new RootIDPair(key, value));
    }
    for (Iterator i = this.dmis.iterator(); i.hasNext();) {
      DmiDescriptor dd = (DmiDescriptor) i.next();
      putNVPair(DMI_ID, dd);
    }
  }

  @Override
  protected boolean hydrateValue(byte name) throws IOException {
    switch (name) {
      case TRANSACTION_TYPE_ID:
        this.transactionType = TxnType.typeFor(getByteValue());
        return true;
      case DNA_ID:
        this.changes.add(getObject(new DNAImpl(this.serializer, false)));
        return true;
      case SERIALIZER_ID:
        this.serializer = (ObjectStringSerializer) getObject(new ObjectStringSerializer());
        return true;
      case LOCK_ID:
        if (this.lockIDs == null) {
          this.lockIDs = new LinkedList();
        }
        this.lockIDs.add(new LockID(getStringValue()));
        return true;
      case NOTIFIED:
        this.notifies.add(this.getObject(new LockContext()));
        return true;
      case CHANGE_ID:
        this.changeID = getLongValue();
        return true;
      case TRANSACTION_ID:
        this.transactionID = new TransactionID(getLongValue());
        return true;
      case COMMITTER_ID:
        this.committerID = ((NodeIDSerializer) getObject(new NodeIDSerializer())).getNodeID();
        return true;
      case GLOBAL_TRANSACTION_ID:
        this.globalTransactionID = new GlobalTransactionID(getLongValue());
        return true;
      case LOW_WATERMARK:
        this.lowWatermark = new GlobalTransactionID(getLongValue());
        return true;
      case ROOT_NAME_ID_PAIR:
        RootIDPair rootIDPair = (RootIDPair) getObject(new RootIDPair());
        this.newRoots.put(rootIDPair.getRootName(), rootIDPair.getRootID());
        return true;
      case DMI_ID:
        DmiDescriptor dd = (DmiDescriptor) getObject(new DmiDescriptor());
        this.dmis.add(dd);
        return true;
      default:
        return false;
    }
  }

  public void initialize(List chges, ObjectStringSerializer aSerializer, LockID[] lids, long cid, TransactionID txID,
                         NodeID client, GlobalTransactionID gtx, TxnType txnType,
                         GlobalTransactionID lowGlobalTransactionIDWatermark, Collection theNotifies, Map roots,
                         DmiDescriptor[] dmiDescs) {
    Assert.eval(lids.length > 0);
    Assert.assertNotNull(txnType);

    this.changes = chges;
    this.lockIDs = Arrays.asList(lids);
    this.changeID = cid;
    this.transactionID = txID;
    this.committerID = client;
    this.transactionType = txnType;
    this.globalTransactionID = gtx;
    this.lowWatermark = lowGlobalTransactionIDWatermark;
    this.serializer = aSerializer;
    this.notifies.addAll(theNotifies);
    this.newRoots.putAll(roots);
    for (int i = 0; i < dmiDescs.length; i++) {
      this.dmis.add(dmiDescs[i]);
    }
  }

  public List getLockIDs() {
    return this.lockIDs;
  }

  public TxnType getTransactionType() {
    return this.transactionType;
  }

  public Collection getObjectChanges() {
    Collection versionizedChanges = new ArrayList(this.changes.size());
    for (Iterator iter = this.changes.iterator(); iter.hasNext();) {
      versionizedChanges.add(new VersionizedDNAWrapper((DNA) iter.next(), this.globalTransactionID.toLong()));

    }
    return versionizedChanges;
  }

  public long getChangeID() {
    return this.changeID;
  }

  public TransactionID getTransactionID() {
    return this.transactionID;
  }

  public NodeID getCommitterID() {
    return this.committerID;
  }

  public GlobalTransactionID getGlobalTransactionID() {
    return this.globalTransactionID;
  }

  public GlobalTransactionID getLowGlobalTransactionIDWatermark() {
    Assert.assertNotNull(this.lowWatermark);
    return this.lowWatermark;
  }

  public Collection addNotifiesTo(List c) {
    c.addAll(this.notifies);
    return c;
  }

  @Override
  public void doRecycleOnRead() {
    // dont recycle yet
  }

  @Override
  protected boolean isOutputStreamRecycled() {
    return true;
  }

  @Override
  public void doRecycleOnWrite() {
    // recycle only those buffers created for this message
    recycleOutputStream();
  }

  public Map getNewRoots() {
    return this.newRoots;
  }

  private static class RootIDPair implements TCSerializable {
    private String   rootName;
    private ObjectID rootID;

    public RootIDPair() {
      super();
    }

    public RootIDPair(String rootName, ObjectID rootID) {
      this.rootName = rootName;
      this.rootID = rootID;
    }

    public void serializeTo(TCByteBufferOutput serialOutput) {
      serialOutput.writeString(this.rootName);
      serialOutput.writeLong(this.rootID.toLong());

    }

    public Object deserializeFrom(TCByteBufferInput serialInput) throws IOException {
      this.rootName = serialInput.readString();
      this.rootID = new ObjectID(serialInput.readLong());
      return this;
    }

    public ObjectID getRootID() {
      return this.rootID;
    }

    public String getRootName() {
      return this.rootName;
    }
  }

  public List getDmiDescriptors() {
    return this.dmis;
  }

}
