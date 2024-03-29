/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections.map;

import org.terracotta.toolkit.cache.ToolkitCacheListener;
import org.terracotta.toolkit.cluster.ClusterNode;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.internal.cache.ToolkitCacheInternal;
import org.terracotta.toolkit.search.QueryBuilder;
import org.terracotta.toolkit.search.attribute.ToolkitAttributeExtractor;

import com.tc.object.ObjectID;
import com.terracotta.toolkit.object.DestroyableToolkitObject;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class LocalReadsToolkitCacheImpl<K, V> implements ValuesResolver<K, V>, ToolkitCacheInternal<K, V>,
    DestroyableToolkitObject {
  private final AtomicReference<ToolkitCacheInternal<K, V>> delegate;
  private final ToolkitCacheInternal<K, V>                  noOpBehaviourResolver;

  public LocalReadsToolkitCacheImpl(AtomicReference<ToolkitCacheInternal<K, V>> delegate,
                                    ToolkitCacheInternal<K, V> noOpBehaviourResolver) {
    this.delegate = delegate;
    this.noOpBehaviourResolver = noOpBehaviourResolver;
  }

  private ToolkitCacheInternal<K, V> getDelegate() {
    ToolkitCacheInternal<K, V> rv = delegate.get();
    if (rv == null) { return noOpBehaviourResolver; }

    return rv;
  }

  @Override
  public String getName() {
    return getDelegate().getName();
  }

  @Override
  public boolean isDestroyed() {
    return getDelegate().isDestroyed();
  }

  @Override
  public void destroy() {
    throw new UnsupportedOperationException();
  }

  @Override
  public V getQuiet(Object key) {
    return getDelegate().unsafeLocalGet(key);
  }

  @Override
  public Map<K, V> getAllQuiet(Collection<K> keys) {
    Map<K, V> rv = new HashMap<K, V>();
    for (K key : keys) {
      rv.put(key, getQuiet(key));
    }
    return rv;
  }

  @Override
  public void putNoReturn(K key, V value, long createTimeInSecs, int maxTTISeconds, int maxTTLSeconds) {
    throw new UnsupportedOperationException();
  }

  @Override
  public V putIfAbsent(K key, V value, long createTimeInSecs, int maxTTISeconds, int maxTTLSeconds) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addListener(ToolkitCacheListener<K> listener) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void removeListener(ToolkitCacheListener<K> listener) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void unpinAll() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isPinned(K key) {
    return getDelegate().isPinned(key);
  }

  @Override
  public void setPinned(K key, boolean pinned) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void removeNoReturn(Object key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void putNoReturn(K key, V value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Map<K, V> getAll(Collection<? extends K> keys) {
    return getAllQuiet((Collection<K>) keys);
  }

  @Override
  public Configuration getConfiguration() {
    return getDelegate().getConfiguration();
  }

  @Override
  public void setConfigField(String name, Serializable value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean containsValue(Object value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ToolkitReadWriteLock createLockForKey(K key) {
    // TODO: return nonstop lock when supporting nonstop for locks.
    return getDelegate().createLockForKey(key);
  }

  @Override
  public void setAttributeExtractor(ToolkitAttributeExtractor attrExtractor) {
    getDelegate().setAttributeExtractor(attrExtractor);
  }

  @Override
  public V putIfAbsent(K key, V value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean remove(Object key, Object value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean replace(K key, V oldValue, V newValue) {
    throw new UnsupportedOperationException();
  }

  @Override
  public V replace(K key, V value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int size() {
    return getDelegate().localSize();
  }

  @Override
  public boolean isEmpty() {
    return getDelegate().localSize() == 0;
  }

  @Override
  public boolean containsKey(Object key) {
    return containsLocalKey(key);
  }

  @Override
  public V get(Object key) {
    return getQuiet(key);
  }

  @Override
  public V put(K key, V value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public V remove(Object key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> m) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Set<K> keySet() {
    return getDelegate().localKeySet();
  }

  @Override
  public Collection<V> values() {
    Map<K, V> allValuesMap = getAllLocalKeyValuesMap();
    return allValuesMap.values();
  }

  @Override
  public Set<java.util.Map.Entry<K, V>> entrySet() {
    Map<K, V> allValuesMap = getAllLocalKeyValuesMap();
    return allValuesMap.entrySet();
  }

  private Map<K, V> getAllLocalKeyValuesMap() {
    Map<K, V> allValuesMap = new HashMap<K, V>(getDelegate().localSize());
    for (K key : getDelegate().keySet()) {
      allValuesMap.put(key, getQuiet(key));
    }
    return allValuesMap;
  }

  @Override
  public Map<Object, Set<ClusterNode>> getNodesWithKeys(Set portableKeys) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void unlockedPutNoReturn(K k, V v, int createTime, int customTTI, int customTTL) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void unlockedRemoveNoReturn(Object k) {
    throw new UnsupportedOperationException();
  }

  @Override
  public V unlockedGet(Object k, boolean quiet) {
    return getQuiet(k);
  }

  @Override
  public void clearLocalCache() {
    // TODO: discuss
    throw new UnsupportedOperationException();
  }

  @Override
  public V unsafeLocalGet(Object key) {
    return getDelegate().unsafeLocalGet(key);
  }

  @Override
  public boolean containsLocalKey(Object key) {
    return getDelegate().containsLocalKey(key);
  }

  @Override
  public int localSize() {
    return getDelegate().localSize();
  }

  @Override
  public Set<K> localKeySet() {
    return getDelegate().localKeySet();
  }

  @Override
  public long localOnHeapSizeInBytes() {
    return getDelegate().localOnHeapSizeInBytes();
  }

  @Override
  public long localOffHeapSizeInBytes() {
    return getDelegate().localOffHeapSizeInBytes();
  }

  @Override
  public int localOnHeapSize() {
    return getDelegate().localOnHeapSize();
  }

  @Override
  public int localOffHeapSize() {
    return getDelegate().localOffHeapSize();
  }

  @Override
  public boolean containsKeyLocalOnHeap(Object key) {
    return getDelegate().containsKeyLocalOnHeap(key);
  }

  @Override
  public boolean containsKeyLocalOffHeap(Object key) {
    return getDelegate().containsKeyLocalOffHeap(key);
  }

  @Override
  public V put(K key, V value, int createTimeInSecs, int customMaxTTISeconds, int customMaxTTLSeconds) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void disposeLocally() {
    // TODO: discuss
    throw new UnsupportedOperationException();
  }

  @Override
  public void removeAll(Set<K> keys) {
    throw new UnsupportedOperationException();
  }

  @Override
  public QueryBuilder createQueryBuilder() {
    return getDelegate().createQueryBuilder();
  }

  @Override
  public void doDestroy() {
    throw new UnsupportedOperationException();
  }

  @Override
  public V get(K key, ObjectID valueOid) {
    // TODO: discuss change in behavior for search here.
    return getDelegate().unsafeLocalGet(key);
  }

  @Override
  public Map<K, V> unlockedGetAll(Collection<K> keys, boolean quiet) {
    return getDelegate().unlockedGetAll(keys, quiet);
  }
}
