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

import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * @author Andrew Brown, andrew.brown@intel.com
 */
public class BoundedInMemoryPendingInterestTableTest {

  private BoundedInMemoryPendingInterestTable instance;

  @Before
  public void before() {
    instance = new BoundedInMemoryPendingInterestTable(5);
  }

  @Test
  public void add() throws Exception {
    Name name = new Name("/a/b/c");
    instance.add(new Interest(name));
    assertTrue(instance.has(name));
  }

  @Test
  public void has() throws Exception {
    Name name = new Name("/a/b/c");
    instance.add(new Interest(name));

    Interest interest = new Interest(new Name("/a/b"));
    interest.setChildSelector(Interest.CHILD_SELECTOR_RIGHT);
    assertTrue(instance.has(interest));
  }

  @Test
  public void extract() throws Exception {
    instance.add(new Interest(new Name("/a")));
    instance.add(new Interest(new Name("/a/b")));
    instance.add(new Interest(new Name("/a/b/c")));

    Collection<Interest> extracted = instance.extract(new Name("/a/b"));
    assertEquals(2, extracted.size()); // TODO not sure about this...
  }
}