package com.tc.util;

import com.tc.object.ObjectID;
import org.junit.Test;

import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Ludovic Orban
 */
public class BasicObjectIDSetTest {

  // see TAB-7213
  @Test
  public void testInsertRangeWithDistantOIDsDoesWork() throws Exception {
    BasicObjectIDSet idSet = new BasicObjectIDSet();

    idSet.insertRange(new ObjectIDSet.BasicRange(Long.MIN_VALUE, new long[]{1}));
    idSet.insertRange(new ObjectIDSet.BasicRange(Long.MAX_VALUE, new long[]{1}));

    assertEquals(2, idSet.size());
    Iterator<ObjectID> iterator = idSet.iterator();
    assertTrue(iterator.hasNext());
    ObjectID oid1 = iterator.next();
    assertEquals(Long.MIN_VALUE, oid1.toLong());
    assertTrue(iterator.hasNext());
    ObjectID oid2 = iterator.next();
    assertEquals(Long.MAX_VALUE, oid2.toLong());
    assertFalse(iterator.hasNext());
  }

}
