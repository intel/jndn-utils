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

import com.intel.jndn.mock.MockFace;
import com.intel.jndn.utils.ContentStore;
import net.named_data.jndn.Name;
import net.named_data.jndn.util.Blob;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Andrew Brown, andrew.brown@intel.com
 */
public class BoundedInMemoryContentStoreTest {
  ContentStore instance = new BoundedInMemoryContentStore(5, 1000);

  @Before
  public void setUp() throws Exception {

  }

  @Test
  public void basicUsage() throws Exception {
    instance.put(new Name("/a"), new Blob("."));

    assertTrue(instance.has(new Name("/a")));
    assertArrayEquals(".".getBytes(), instance.get(new Name("/a")).getImmutableArray());
  }

  @Test
  public void replacement() throws Exception {
    instance.put(new Name("/a"), new Blob("."));
    instance.put(new Name("/a/b"), new Blob(".."));
    instance.put(new Name("/a/b/c"), new Blob("..."));
    instance.put(new Name("/a/b/c/d"), new Blob("...."));
    instance.put(new Name("/a/b/c/d/e"), new Blob("...."));
    assertTrue(instance.has(new Name("/a")));

    instance.put(new Name("/replace/oldest"), new Blob("."));

    assertFalse(instance.has(new Name("/a")));
    assertTrue(instance.has(new Name("/replace/oldest")));
  }

  @Test
  public void push() throws Exception {
    MockFace face = new MockFace();
    Name name = new Name("/a");
    instance.put(name, new Blob("."));

    instance.push(face, name);

    assertEquals(1, face.sentData.size());
    assertEquals(name.appendSegment(0), face.sentData.get(0).getName()); // TODO this should probably be smarter and avoid appending segments if not needed
  }

  @Test
  public void clear() throws Exception {
    instance.put(new Name("/a"), new Blob("."));
    assertTrue(instance.has(new Name("/a")));

    instance.clear();

    assertFalse(instance.has(new Name("/a")));
  }

}