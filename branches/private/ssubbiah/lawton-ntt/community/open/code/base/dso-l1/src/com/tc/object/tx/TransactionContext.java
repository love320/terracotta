/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object.tx;

import com.tc.object.lockmanager.api.LockID;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TransactionContext {
  TxnType          type;
  LockID           lockID;
  private LockID[] lockIDs;

  public TransactionContext(LockID lockID, TxnType type, LockID[] lockIDs) {
    this.type = type;
    this.lockID = lockID;
    this.lockIDs = lockIDs;
  }

  public TxnType getType() {
    return type;
  }

  public LockID getLockID() {
    return lockID;
  }

  public LockID[] getAllLockIDs() {
    return lockIDs;
  }

  public void removeLock(LockID id) {
    List list = new ArrayList(Arrays.asList(lockIDs));
    list.remove(id);
    lockIDs = new LockID[list.size()];
    for (int i=0; i<lockIDs.length; i++) {
      lockIDs[i] = (LockID)list.get(i);
    }
  }
}