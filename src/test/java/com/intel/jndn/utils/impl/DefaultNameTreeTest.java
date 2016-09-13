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

import com.intel.jndn.utils.NameTree;
import net.named_data.jndn.Name;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Andrew Brown, andrew.brown@intel.com
 */
public class DefaultNameTreeTest {

  private NameTree<String> instance;

  @Before
  public void setUp() throws Exception {
    instance = DefaultNameTree.newRootTree();
    instance.insert(new Name("/a/b/c"), ".");
    instance.insert(new Name("/a/b/d"), "..");
    instance.insert(new Name("/a/e"), "...");
  }

  @Test
  public void content() throws Exception {
    // TODO
  }

  @Test
  public void retrieveName() throws Exception {
    assertEquals("/a", instance.find(new Name("/a")).get().fullName().toString());
    assertEquals("/a/b", instance.find(new Name("/a/b")).get().fullName().toString());
    assertEquals("/a/b/c", instance.find(new Name("/a/b/c")).get().fullName().toString());
  }

  @Test
  public void children() throws Exception {
    // TODO
  }

  @Test
  public void parent() throws Exception {
    assertNull(instance.parent());
  }

  @Test
  public void find() throws Exception {
    assertEquals(2, instance.find(new Name("/a/b")).get().children().size());
  }

  @Test
  public void findDeep() throws Exception {
    instance.insert(new Name("/a/e/x/y/z"), "...");
    assertEquals(2, instance.find(new Name("/a")).get().children().size());
    assertEquals(1, instance.find(new Name("/a/e")).get().children().size());
    assertEquals(1, instance.find(new Name("/a/e/x")).get().children().size());
    assertEquals(1, instance.find(new Name("/a/e/x/y")).get().children().size());
    assertEquals(0, instance.find(new Name("/a/e/x/y/z")).get().children().size());
  }

  @Test
  public void insert() throws Exception {
    // TODO
  }

  @Test
  public void delete() throws Exception {
    // TODO
  }

  @Test
  public void count() throws Exception {
    // TODO
  }
}