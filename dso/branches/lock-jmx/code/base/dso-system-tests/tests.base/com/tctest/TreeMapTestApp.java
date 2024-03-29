/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractTransparentApp;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

public class TreeMapTestApp extends AbstractTransparentApp {

  // plain old TreeMap
  private final TreeMap       map           = new TreeMap();

  // Use a comparator with a shared TreeMap too. If the comparator doesn't make it across VMs,
  // then we should get some ClassCastExceptions
  private final TreeMap       map2          = new TreeMap(new WrappedStringComparator());

  private final CyclicBarrier barrier;

  private final SubMapKey     subMapKeyRoot = new SubMapKey(0);
  
  private final int loopcount = 200;

  public TreeMapTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(getParticipantCount());
  }

  public void run() {
    try {
      for(int i = 0; i < loopcount; ++i) {
        System.out.println("*** TreeMap LoopCount:"+i);
        clear();
        run0();
        run1();
      }
    } catch (Throwable t) {
      notifyError(t);
    }
  }

  private void run0() throws Exception {
    String me = getApplicationId();

    synchronized (map) {
      map.put(me, me + "-value");
    }

    synchronized (map2) {
      map2.put(new WrappedString(me), me + "-value");
    }

    barrier.barrier();

    validate(map, getParticipantCount(), null);
    validate(map2, getParticipantCount(), new WrappedStringComparator());

    barrier.barrier();

    synchronized (map) {
      Object removed = map.remove(me);
      Assert.assertNotNull(removed);
    }

    synchronized (map2) {
      Object removed = map2.remove(new WrappedString(me));
      Assert.assertNotNull(removed);
    }

    barrier.barrier();

    Assert.assertEquals(0, getMapSize(map));
    Assert.assertEquals(0, getMapSize(map2));

    barrier.barrier();

    synchronized (map) {
      map.put(me, me + "-value");
    }

    synchronized (map2) {
      map2.put(new WrappedString(me), me + "-value");
    }

    barrier.barrier();

    synchronized (map) {
      if (map.size() == getParticipantCount()) {
        map.clear();
      } else {
        Assert.assertEquals(0, map.size());
      }
    }

    synchronized (map2) {
      if (map2.size() == getParticipantCount()) {
        map2.clear();
      } else {
        Assert.assertEquals(0, map2.size());
      }
    }

    barrier.barrier();

    synchronized (map) {
      map.put(me, "initial");
    }

    synchronized (map2) {
      map2.put(new WrappedString(me), "initial");
    }

    synchronized (map) {
      Object prev = map.put(me, "replaced");
      Assert.assertEquals("initial", prev);
    }

    synchronized (map2) {
      Object prev = map2.put(new WrappedString(me), "replaced");
      Assert.assertEquals("initial", prev);
    }

    barrier.barrier();

    Assert.assertEquals(getParticipantCount(), getMapSize(map));
    Assert.assertEquals(getParticipantCount(), getMapSize(map2));

    // synchronization here to have dso previous transactions completed for the object.
    synchronized (map) {
      for (Iterator i = map.values().iterator(); i.hasNext();) {
        Assert.assertEquals("replaced", i.next());
      }
    }

    synchronized (map2) {
      for (Iterator i = map2.values().iterator(); i.hasNext();) {
        Assert.assertEquals("replaced", i.next());
      }
    }

    barrier.barrier();
  }

  private void run1() throws Exception {
    clear();
    initializeMaps();

    // subMap() testing.
    synchronized (map) {
      if (map.size() == getParticipantCount()) {
        if (map.size() > 2) {
          Object fromKey = new Integer(0);
          Object toKey = new Integer(10);
          Map subMap = map.subMap(fromKey, toKey);
          subMap.put(new Integer(1), "subMap-value");
        }
      } else {
        Assert.assertEquals(getParticipantCount() + 1, map.size());
        Assert.assertTrue(map.get(new Integer(1)).equals("subMap-value"));
      }
    }

    synchronized (map2) {
      if (map2.size() == getParticipantCount()) {
        if (map2.size() > 2) {
          Object fromKey = new WrappedString(0);
          Object toKey = new WrappedString(10);
          Map subMap = map2.subMap(fromKey, toKey);
          subMap.put(new WrappedString(1), "subMap-value");
        }
      } else {
        Assert.assertEquals(getParticipantCount() + 1, map2.size());
        Assert.assertTrue(map2.get(new WrappedString(1)).equals("subMap-value"));
      }
    }

    barrier.barrier();

    clear();
    initializeMaps();

    // headMap() testing.
    synchronized (map) {
      if (map.size() == getParticipantCount()) {
        if (map.size() > 2) {
          Object toKey = new Integer(2);
          Map headMap = map.headMap(toKey);
          headMap.put(new Integer(1), "subMap-value");
        }
      } else {
        Assert.assertEquals(getParticipantCount() + 1, map.size());
        Assert.assertTrue(map.get(new Integer(1)).equals("subMap-value"));
      }
    }

    synchronized (map2) {
      if (map2.size() == getParticipantCount()) {
        if (map2.size() > 2) {
          Object toKey = new WrappedString(2);
          Map headMap = map2.headMap(toKey);
          headMap.put(new WrappedString(1), "subMap-value");
        }
      } else {
        Assert.assertEquals(getParticipantCount() + 1, map2.size());
        Assert.assertTrue(map2.get(new WrappedString(1)).equals("subMap-value"));
      }
    }

    barrier.barrier();

    clear();
    initializeMaps();

    // tailMap() testing.
    synchronized (map) {
      if (map.size() == getParticipantCount()) {
        if (map.size() > 2) {
          Object fromKey = new Integer(0);
          Map tailMap = map.tailMap(fromKey);
          tailMap.put(new Integer(1), "subMap-value");
        }
      } else {
        Assert.assertEquals(getParticipantCount() + 1, map.size());
        Assert.assertTrue(map.get(new Integer(1)).equals("subMap-value"));
      }
    }

    synchronized (map2) {
      if (map2.size() == getParticipantCount()) {
        if (map2.size() > 2) {
          Object fromKey = new WrappedString(0);
          Map tailMap = map2.tailMap(fromKey);
          tailMap.put(new WrappedString(1), "subMap-value");
        }
      } else {
        Assert.assertEquals(getParticipantCount() + 1, map2.size());
        Assert.assertTrue(map2.get(new WrappedString(1)).equals("subMap-value"));
      }
    }

    barrier.barrier();

    clear();
    initializeMaps();

    // tailMap() clear testing.
    synchronized (map) {
      if (map.size() == getParticipantCount()) {
        Object fromKey = new Integer(0);
        Map tailMap = map.tailMap(fromKey);
        tailMap.clear();
      } else {
        Assert.assertEquals(0, map.size());
      }
    }

    synchronized (map2) {
      if (map2.size() == getParticipantCount()) {
        Object fromKey = new WrappedString(0);
        Map tailMap = map2.tailMap(fromKey);
        tailMap.clear();
      } else {
        Assert.assertEquals(0, map2.size());
      }
    }

    barrier.barrier();

  }
  
  private int getMapSize(TreeMap m) {
    synchronized(m) {
      return (m.size());
    }
  }

  private void clear() throws Exception {
    synchronized (map) {
      map.clear();
    }
    synchronized (map2) {
      map2.clear();
    }
    barrier.barrier();
  }

  private void initializeMaps() throws Exception {
    String me = getApplicationId();

    synchronized (subMapKeyRoot) {
      if (subMapKeyRoot.getKey() != 0) {
        subMapKeyRoot.setKey(0);
      }
    }
    barrier.barrier();

    synchronized (map) {
      int key = subMapKeyRoot.getKey();
      map.put(new Integer(key), me + "-value");
      subMapKeyRoot.setKey(key + 2);
    }
    barrier.barrier();
    synchronized (subMapKeyRoot) {
      if (subMapKeyRoot.getKey() != 0) {
        subMapKeyRoot.setKey(0);
      }
    }
    barrier.barrier();
    synchronized (map2) {
      int key = subMapKeyRoot.getKey();
      map2.put(new WrappedString(key), me + "-value");
      subMapKeyRoot.setKey(key + 2);
    }
    barrier.barrier();
  }

  private static void validate(Map map, int count, Comparator comparator) {
    int expect = count;
    Assert.assertEquals(expect, map.size());

    TreeMap compare = comparator == null ? new TreeMap() : new TreeMap(comparator);
    compare.putAll(map);

    Iterator sharedIter = map.entrySet().iterator();
    Iterator localIter = compare.entrySet().iterator();

    while (true) {
      Entry sharedEntry = (Entry) sharedIter.next();
      Entry localEntry = (Entry) localIter.next();

      Object sharedKey = sharedEntry.getKey();
      Object localKey = localEntry.getKey();
      Assert.assertEquals(localKey, sharedKey);

      Object sharedValue = sharedEntry.getValue();
      Object localValue = localEntry.getValue();
      Assert.assertEquals(localValue, sharedValue);

      if (sharedIter.hasNext()) {
        Assert.assertTrue(localIter.hasNext());
      } else {
        break;
      }
    }

  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    TransparencyClassSpec spec = config.getOrCreateSpec(CyclicBarrier.class.getName());
    config.addWriteAutolock("* " + CyclicBarrier.class.getName() + "*.*(..)");

    String testClass = TreeMapTestApp.class.getName();
    spec = config.getOrCreateSpec(testClass);
    
    config.addIncludePattern(testClass + "$*");

    String methodExpression = "* " + testClass + "*.getMapSize(..)";
    config.addReadAutolock(methodExpression);
//    methodExpression = "* " + testClass + "*.*(..)";
//    config.addWriteAutolock(methodExpression);
    methodExpression = "* " + testClass + ".run*(..)";
    config.addWriteAutolock(methodExpression);
    methodExpression = "* " + testClass + ".clear(..)";
    config.addWriteAutolock(methodExpression);
    methodExpression = "* " + testClass + ".initializeMaps(..)";
    config.addWriteAutolock(methodExpression);

    spec.addRoot("map", "map");
    spec.addRoot("map2", "map2");
    spec.addRoot("barrier", "barrier");
    spec.addRoot("subMapKeyRoot", "subMapKeyRoot");
    
    config.addIncludePattern(WrappedStringComparator.class.getName());
    config.addIncludePattern(WrappedString.class.getName());
  }

  // The main purpose of this class is that it does NOT implement Comparable
  private static class WrappedString {
    private final String string;

    WrappedString(String string) {
      this.string = string;
    }

    WrappedString(int i) {
      this.string = String.valueOf(i);
    }

    String getString() {
      return this.string;
    }
  }

  private static class WrappedStringComparator implements Comparator {

    public int compare(Object o1, Object o2) {
      WrappedString ws1 = (WrappedString) o1;
      WrappedString ws2 = (WrappedString) o2;
      return ws1.getString().compareTo(ws2.getString());
    }

  }

  /**
   * The main purpose of this class is for to generate the key for the subMap(), headMap(), and tailMap() testing.
   */
  private static class SubMapKey {
    private int key;

    public SubMapKey(int key) {
      this.key = key;
    }

    public int getKey() {
      return key;
    }

    public void setKey(int key) {
      this.key = key;
    }
  }
}
