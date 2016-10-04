/*
 * jndn-utils
 * Copyright (c) 2016, Intel Corporation.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms and conditions of the GNU Lesser General Public License,
 * version 3, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details.
 */

package com.intel.jndn.utils.impl;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Linked hash map exposing the earliest and latest entries added; it is bounded to a configurable size to save memory.
 * When benchmarked against CircularBufferCache, this class had slower insertions but faster reads and was therefore
 * retained for use.
 * <p>
 * It limits the amount of memory by replacing the oldest added element; this involves overriding the LinkedHashMap
 * implementation's removeEldestEntry() method; see the <a href="https://docs.oracle.com/javase/8/docs/api/java/util/LinkedHashMap.html#removeEldestEntry-java.util.Map.Entry-">Javadoc
 * entry</a> for more information.
 * <p>
 * Additionally, we implemented Josh Bloch's item 16 of Effective Java so that calls are forwarded to the underlying
 * LinkedHashMap. This allows us to decorate with some custom behavior and synchronize as we need.
 * <p>
 * This class is coarsely thread-safe; every public method is synchronized for one-at-a-time access to the underlying
 * map.
 *
 * @author Andrew Brown, andrew.brown@intel.com
 */
public class BoundedLinkedMap<K, V> implements Map<K, V> {
  private final LinkedHashMap<K, V> map;
  private K latest;

  /**
   * @param maxSize the maximum allowed number of records to store
   */
  public BoundedLinkedMap(int maxSize) {
    this.map = new LinkedHashMap<K, V>(maxSize) {
      @Override
      public boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > maxSize;
      }
    };
  }

  /**
   * @return the earliest key added to this set or null if none are added
   */
  public synchronized K earliest() {
    for (K key : map.keySet()) {
      return key; // the LinkedHashMap guarantees iteration in order of insertion
    }
    return null;
  }

  /**
   * @return the latest key added to this set or null if none are added
   */
  public synchronized K latest() {
    return latest;
  }

  @Override
  public synchronized V put(K key, V value) {
    latest = key;
    return map.put(key, value);
  }

  @Override
  public synchronized int size() {
    return map.size();
  }

  @Override
  public synchronized boolean isEmpty() {
    return map.isEmpty();
  }

  @Override
  public synchronized boolean containsKey(Object key) {
    return map.containsKey(key);
  }

  @Override
  public synchronized boolean containsValue(Object value) {
    return map.containsValue(value);
  }

  @Override
  public synchronized V get(Object key) {
    return map.get(key);
  }


  @Override
  public synchronized V remove(Object key) {
    V value = map.remove(key);
    if (key.equals(latest)) {
      latest = findLatest(map);
    }
    return value;
  }

  @Override
  public synchronized void putAll(Map<? extends K, ? extends V> m) {
    map.putAll(m);
    latest = findLatest(map);
  }

  @Override
  public synchronized void clear() {
    map.clear();
    latest = null;
  }

  @Override
  public synchronized Set<K> keySet() {
    return map.keySet();
  }

  @Override
  public synchronized Collection<V> values() {
    return map.values();
  }

  @Override
  public synchronized Set<java.util.Map.Entry<K, V>> entrySet() {
    return map.entrySet();
  }

  @Override
  public String toString() {
    return map.toString();
  }

  /**
   * To find the latest key in a LinkedHashMap, iterate and return the last one found. The LinkedHashMap guarantees
   * iteration in order of insertion
   *
   * @param m the map to inspect
   * @return the latest key added to the map
   */
  private K findLatest(LinkedHashMap<K, V> m) {
    K newLatest = null;
    for (K key : m.keySet()) {
      newLatest = key;
    }
    return newLatest;
  }
}
