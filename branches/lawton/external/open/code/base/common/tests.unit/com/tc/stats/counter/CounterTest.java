/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.stats.counter;

import java.util.Random;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedLong;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedRef;
import junit.framework.TestCase;

public class CounterTest extends TestCase {

  public void testInitialValue() {
    Counter counter = new CounterImpl();
    assertEquals(0L, counter.getValue());

    counter = new CounterImpl(42L);
    assertEquals(42L, counter.getValue());
  }

  public void testConcurrency() throws InterruptedException {
    final Counter counter = new CounterImpl();
    final SynchronizedLong local = new SynchronizedLong(0L);
    final SynchronizedRef error = new SynchronizedRef(null);

    Thread[] threads = new Thread[10];
    for (int i = 0; i < threads.length; i++) {
      threads[i] = new Thread() {
        public void run() {
          try {
            Random random = new Random(hashCode());
            for (int n = 0; n < 100000; n++) {
              int operation = random.nextInt(4);

              switch (operation) {
                case 0: {
                  local.decrement();
                  counter.decrement();
                  break;
                }
                case 1: {                  
                  local.increment();
                  counter.increment();
                  break;
                }
                case 2: {
                  long amount = random.nextLong();
                  local.subtract(amount);
                  counter.decrement(amount);
                  break;
                }
                case 3: {
                  long amount = random.nextLong();
                  local.add(amount);
                  counter.increment(amount);
                  break;
                }
                default: {
                  throw new RuntimeException("operation " + operation);
                }
              }
            }
          } catch (Throwable t) {
            t.printStackTrace();
            error.set(t);
          }
        }
      };
      threads[i].start();
    }

    for (int i = 0; i < threads.length; i++) {
      threads[i].join();
    }

    if (error.get() != null) {
      fail(error.get().toString());
    }

    assertEquals(local.get(), counter.getValue());
  }

  
  public void testMinMax() {
    Counter counter = new CounterImpl();
    assertEquals(0, counter.getMinValue());
    assertEquals(0, counter.getMaxValue());
    
    counter.increment();
    assertEquals(0, counter.getMinValue());
    assertEquals(1, counter.getMaxValue());
    
    counter.decrement();
    assertEquals(0, counter.getMinValue());
    assertEquals(1, counter.getMaxValue());
    counter.decrement();
    assertEquals(-1, counter.getMinValue());
    assertEquals(1, counter.getMaxValue());
    
    counter.increment(10);
    assertEquals(-1, counter.getMinValue());
    assertEquals(9, counter.getMaxValue());
    
    counter.decrement(20);
    assertEquals(-11, counter.getMinValue());
    assertEquals(9, counter.getMaxValue());
    
    counter.setValue(100);
    assertEquals(-11, counter.getMinValue());
    assertEquals(100, counter.getMaxValue());
    
    counter.setValue(-100);
    assertEquals(-100, counter.getMinValue());
    assertEquals(100, counter.getMaxValue());
    
    counter.getAndSet(5);
    assertEquals(-100, counter.getMinValue());
    assertEquals(100, counter.getMaxValue());      
  }
  
  
  public void test() {
    Counter counter = new CounterImpl();
    assertEquals(0L, counter.getValue());

    counter.decrement();
    assertEquals(-1L, counter.getValue());

    counter.increment();
    assertEquals(0L, counter.getValue());

    counter.decrement(10L);
    assertEquals(-10L, counter.getValue());

    counter.increment(10L);
    assertEquals(0L, counter.getValue());

    counter.increment(-10L);
    assertEquals(-10L, counter.getValue());

    counter.decrement(-10L);
    assertEquals(0L, counter.getValue());

    counter.setValue(Long.MAX_VALUE);
    assertEquals(Long.MAX_VALUE, counter.getValue());

    counter.setValue(Long.MIN_VALUE);
    assertEquals(Long.MIN_VALUE, counter.getValue());

    counter.setValue(0L);
    assertEquals(0L, counter.getValue());

    counter.increment();
    counter.increment();
    counter.increment();
    long value = counter.getAndSet(42L);
    assertEquals(3L, value);
    assertEquals(42L, counter.getValue());
  }

}
