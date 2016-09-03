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

package com.intel.jndn.utils.pubsub;

import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Andrew Brown, andrew.brown@intel.com
 */
public class ForLoopPendingInterestTable implements PendingInterestTable {
  Map<Name, Interest> table = new ConcurrentHashMap<>();

  @Override
  public void add(Interest interest) {
    table.put(interest.getName(), interest);
  }

  @Override
  public boolean has(Interest interest) {
    // TODO must handle more complex logic (selectors)
    return has(interest.getName());
  }

  public boolean has(Name name) {
    return table.containsKey(name);
  }

  @Override
  public Collection<Interest> extract(Name name) {
    // TODO more complexity to return multiple results
    return has(name) ? Collections.singleton(table.get(name)) : Collections.emptyList();
  }
}
