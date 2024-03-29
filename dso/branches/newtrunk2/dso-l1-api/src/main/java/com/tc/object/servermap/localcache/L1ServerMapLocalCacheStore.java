/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache;

import java.util.Set;

/**
 * The backing Cache Store for the Local Cache present in TCObjectServerMapImpl
 */
public interface L1ServerMapLocalCacheStore<K, V> {

  /**
   * Put an entry in the backing map
   * <p/>
   * The behavior depends on the putType - the entry may be pinned on put, size may not increment even on put etc
   * 
   * @return the old value if present
   */
  public V put(K key, V value, PutType putType);

  /**
   * @return the value if present
   */
  public V get(K key);

  /**
   * Remove an entry in the backing map<br>
   * 
   * @return the old value if present
   */
  public V remove(K key, RemoveType removeType);

  /**
   * Add a listener which will get called when <br>
   * 1) capacity eviction evicts entries from map<br>
   * 2) evict (count) method evicts entries from map<br>
   */
  public boolean addListener(L1ServerMapLocalCacheStoreListener<K, V> listener);

  /**
   * Removes the added listener
   */
  public boolean removeListener(L1ServerMapLocalCacheStoreListener<K, V> listener);

  /**
   * Clear the map
   */
  public void clear();

  /**
   * @return key set for this map
   */
  public Set getKeySet();

  /**
   * Size does not take into consideration for elements inserted with {@link PutType#incrementSizeOnPut()} returning
   * false
   * 
   * @return size of the map
   */
  public int size();

  /**
   * Bytes this local cache is taking up on heap.
   */
  public long onHeapSizeInBytes();

  /**
   * Bytes this local cache is taking up off heap.
   */
  public long offHeapSizeInBytes();

  /**
   * Get the number of items on the local heap
   */
  public int onHeapSize();

  /**
   * Get the number of items on the local offheap
   */
  public int offHeapSize();

  /**
   * Unpin entry so that it is eligible for eviction
   * 
   * @param value
   */
  public void unpinEntry(K key, V value);

  /**
   * Max elements in memory
   */
  public int getMaxElementsInMemory();

  /**
   * Dispose of this local cache store
   */
  public void dispose();

  /**
   * Check if the key is available on heap
   */
  public boolean containsKeyOnHeap(K key);

  /**
   * Check if the key is available off heap
   */
  public boolean containsKeyOffHeap(K key);

  /**
   * get L1ServerMapLocalCacheLockProvider to lock/unlock this store
   */
  public L1ServerMapLocalCacheLockProvider getLockProvider();

}
