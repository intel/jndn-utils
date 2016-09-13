/*
 * jndn-utils
 * Copyright (c) 2015, Intel Corporation.
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
package com.intel.jndn.utils.repository.impl;

import com.intel.jndn.utils.Repository;
import java.util.ArrayList;
import java.util.List;
import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;

/**
 * Store {@link Data} packets in a linked list and iterate over the list to find
 * the best match; this is a subset of the functionality provided in
 * {@link net.named_data.jndn.util.MemoryContentCache} and borrows the matching
 * logic from there.
 *
 * @author Andrew Brown, andrew.brown@intel.com
 */
public class ForLoopRepository implements Repository {

  private final List<Record> storage = new ArrayList<>();

  /**
   * {@inheritDoc}
   */
  @Override
  public void put(Data data) {
    storage.add(new Record(data));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Data get(Interest interest) throws DataNotFoundException {
    Name.Component selectedComponent = null;
    Data selectedData = null;
    for (Record record : storage) {
      if (interest.matchesName(record.data.getName())) {
        if (hasNoChildSelector(interest) && hasAcceptableFreshness(interest, record)) {
          selectedData = record.data;
        } else {
          Name.Component component = getNextComponentAfterLastInterestComponent(record.data, interest);

          boolean gotBetterMatch = false;
          if (selectedData == null) {
            // Save the first match.
            gotBetterMatch = true;
          } else {
            if (interest.getChildSelector() == Interest.CHILD_SELECTOR_LEFT) {
              // Leftmost child.
              if (component.compare(selectedComponent) < 0) {
                gotBetterMatch = true;
              }
            } else {
              // Rightmost child.
              if (component.compare(selectedComponent) > 0) {
                gotBetterMatch = true;
              }
            }
          }

          if (gotBetterMatch && hasAcceptableFreshness(interest, record)) {
            selectedComponent = component;
            selectedData = record.data;
          }
        }
      }
    }

    if (selectedData != null) {
      // We found the leftmost or rightmost child.
      return selectedData;
    } else {
      throw new DataNotFoundException();
    }
  }

  /**
   * @param content the content to check (e.g. /a/b/c)
   * @param interest the interest to check from (e.g. /a/b)
   * @return the next component from a Data packet after specified Interest
   * components (e.g. c); if the Data is not longer than the Interest, return an
   * empty component.
   */
  private static Name.Component getNextComponentAfterLastInterestComponent(Data content, Interest interest) {
    if (content.getName().size() > interest.getName().size()) {
      return content.getName().get(interest.getName().size());
    } else {
      return new Name.Component();
    }
  }

  /**
   * @param interest the {@link Interest} to check
   * @return true if the {@link Interest} has no child selector
   */
  private static boolean hasNoChildSelector(Interest interest) {
    return interest.getChildSelector() < 0;
  }

  /**
   * Check if a record is fresh.
   *
   * @param record the record to check
   * @return true if the record is fresh
   */
  private boolean isFresh(Record record) {
    double period = record.data.getMetaInfo().getFreshnessPeriod();
    return period < 0 || record.addedAt + (long) period > System.currentTimeMillis();
  }

  /**
   * Based on an Interest's requested freshness, determine if the record has an
   * acceptable freshness.
   *
   * @param interest the Interest, with mustBeFresh set to true/false
   * @param record the record to check
   * @return true if the Interest does not require a fresh record or if the
   * record is fresh
   */
  private boolean hasAcceptableFreshness(Interest interest, Record record) {
    return !interest.getMustBeFresh() || isFresh(record);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean satisfies(Interest interest) {
    for (Record record : storage) {
      if (interest.matchesName(record.data.getName()) && hasAcceptableFreshness(interest, record)) {
        return true;
      }
    }
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void cleanup() {
    for (int i = storage.size() - 1; i >= 0; i--) {
      if (!isFresh(storage.get(i))) {
        synchronized (storage) {
          storage.remove(i);
        }
      }
    }
  }

  /**
   * Helper data structure
   */
  private class Record {

    final Data data;
    final long addedAt;

    Record(Data data) {
      this.data = data;
      this.addedAt = System.currentTimeMillis();
    }
  }
}
