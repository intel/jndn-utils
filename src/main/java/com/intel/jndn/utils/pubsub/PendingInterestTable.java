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

/**
 * @author Andrew Brown, andrew.brown@intel.com
 */
public interface PendingInterestTable {
  /**
   * @param interest the PIT entry to add
   */
  void add(Interest interest);

  /**
   * @param interest the incoming interest to match against
   * @return true if the interest matches an entry already in the PIT
   */
  boolean has(Interest interest);

  /**
   * @param name the name to match against
   * @return the PIT entries matching a name, removing them from the PIT
   */
  Collection<Interest> extract(Name name);
}
