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

import com.intel.jndn.utils.PendingInterestTable;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * TODO use NameTree for storage? or Set and override Interest equals()
 *
 * @author Andrew Brown, andrew.brown@intel.com
 */
public class BoundedInMemoryPendingInterestTable implements PendingInterestTable {
  private static final Logger LOGGER = Logger.getLogger(BoundedInMemoryPendingInterestTable.class.getName());
  private final BoundedLinkedMap<Name, Interest> table;

  public BoundedInMemoryPendingInterestTable(int maxSize) {
    this.table = new BoundedLinkedMap<>(maxSize);
  }

  @Override
  public void add(Interest interest) {
    LOGGER.log(Level.INFO, "Adding pending interest: {0}", interest.toUri());
    table.put(interest.getName(), interest);
  }

  @Override
  public boolean has(Interest interest) {
    if (interest.getChildSelector() != -1) {
      for (Name name : table.keySet()) {
        // TODO this logic must be more complex; must match selectors as well
        if (interest.matchesName(name)) {
          return true;
        }
      }
      return false;
    } else {
      return has(interest.getName());
    }
  }

  public boolean has(Name name) {
    return table.containsKey(name);
  }

  @Override
  public Collection<Interest> extract(Name name) {
    return table.values().stream().filter(i -> i.matchesName(name)).collect(Collectors.toList());
  }
}
