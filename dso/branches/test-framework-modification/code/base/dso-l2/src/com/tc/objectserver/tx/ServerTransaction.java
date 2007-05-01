/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.dmi.DmiDescriptor;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.gtx.GlobalTransaction;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.TxnBatchID;
import com.tc.object.tx.TxnType;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents an atomic change to the states of objects on the server
 * 
 * @author steve
 */

public interface ServerTransaction extends GlobalTransaction {

  public TxnBatchID getBatchID();

  public ObjectStringSerializer getSerializer();

  public LockID[] getLockIDs();

  public ChannelID getChannelID();

  public TransactionID getTransactionID();

  public ServerTransactionID getServerTransactionID();
  
  public List getChanges();

  public Map getNewRoots();

  public TxnType getTransactionType();

  public Collection getObjectIDs();
  
  public Set getNewObjectIDs();

  public Collection addNotifiesTo(List list);

  public DmiDescriptor[] getDmiDescriptors();

  /*
   * This method returns true if the transaction arrives in the passive server. I see that this method name/signature
   * will change and become more sane/complex in future.
   */
  public boolean isPassive();

}
