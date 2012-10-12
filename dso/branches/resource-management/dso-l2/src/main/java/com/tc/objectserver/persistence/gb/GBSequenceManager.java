package com.tc.objectserver.persistence.gb;

import org.terracotta.corestorage.KeyValueStorage;
import org.terracotta.corestorage.KeyValueStorageConfig;
import org.terracotta.corestorage.heap.KeyValueStorageConfigImpl;

import com.tc.util.sequence.MutableSequence;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author tim
 */
public class GBSequenceManager {

  private final ConcurrentMap<String, GBSequence> createdSequences =
          new ConcurrentHashMap<String, GBSequence>();
  private final KeyValueStorage<String, Long> sequenceMap;

  public GBSequenceManager(KeyValueStorage<String, Long> sequenceMap) {
    this.sequenceMap = sequenceMap;
  }

  public MutableSequence getSequence(String name) {
    GBSequence sequence = createdSequences.get(name);
    if (sequence == null) {
      sequence = new GBSequence(sequenceMap, name);
      GBSequence racer = createdSequences.putIfAbsent(name, sequence);
      if (racer != null) {
        sequence = racer;
      }
    }
    return sequence;
  }

  public static KeyValueStorageConfig<String, Long> config() {
    KeyValueStorageConfig<String, Long> config = new KeyValueStorageConfigImpl<String, Long>(String.class, Long.class);
    config.setKeySerializer(StringSerializer.INSTANCE);
    config.setValueSerializer(LongSerializer.INSTANCE);
    return config;
  }

  private static class GBSequence implements MutableSequence {

    private final KeyValueStorage<String, Long> sequenceMap;
    private final String name;

    GBSequence(KeyValueStorage<String, Long> sequenceMap, String name) {
      this.name = name;
      this.sequenceMap = sequenceMap;
      Long current = sequenceMap.get(name);
      if (current == null) {
        current = 0L;
        sequenceMap.put(name, current);
      }
    }

    @Override
    public String getUID() {
      return name;
    }

    @Override
    public synchronized long nextBatch(long batchSize) {
      Long r = sequenceMap.get(name);
      sequenceMap.put(name, r + batchSize);
      return r;
    }

    @Override
    public synchronized void setNext(long next) {
      if (next < sequenceMap.get(name)) {
        throw new AssertionError("next=" + next + " current=" + sequenceMap.get(name));
      }
      sequenceMap.put(name, next);
    }

    @Override
    public long next() {
      return nextBatch(1);
    }

    @Override
    public synchronized long current() {
      return sequenceMap.get(name);
    }
  }
}
