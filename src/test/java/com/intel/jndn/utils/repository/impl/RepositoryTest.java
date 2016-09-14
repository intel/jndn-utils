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
import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import org.junit.Test;

import static com.intel.jndn.utils.repository.impl.RepoHelper.*;
import static org.junit.Assert.*;

/**
 * Extend this in descendant tests.
 *
 * @author Andrew Brown, andrew.brown@intel.com
 */
public abstract class RepositoryTest {

  Repository instance;

  @Test
  public void testGetAndPut() throws DataNotFoundException {
    instance.put(buildData("/a/b/c"));
    Data data = instance.get(buildInterest("/a/b/c"));
    assertEquals("...", data.getContent().toString());
  }

  @Test
  public void testThatChildSelectorsRetrieve() throws DataNotFoundException {
    instance.put(buildData("/a/b/c"));
    instance.put(buildData("/a/b/c/e"));
    instance.put(buildData("/a/b/d"));

    Interest interest = buildInterest("/a/b");
    interest.setMustBeFresh(false);
    interest.setChildSelector(Interest.CHILD_SELECTOR_RIGHT);
    Data data = instance.get(interest);
    assertEquals("/a/b/d", data.getName().toUri());

    Interest interest2 = buildInterest("/a/b/c");
    interest.setMustBeFresh(false);
    interest2.setChildSelector(Interest.CHILD_SELECTOR_RIGHT);
    Data data2 = instance.get(interest2);
    assertEquals("/a/b/c/e", data2.getName().toUri());
  }
  
  @Test
  public void testChildSelectorsOnExactMatch() throws DataNotFoundException{
    instance.put(buildData("/a/b/c"));
    instance.put(buildData("/a/b/d"));
    
    Interest interest = buildInterest("/a/b/c").setChildSelector(Interest.CHILD_SELECTOR_LEFT);
    assertTrue(instance.satisfies(interest));
    assertEquals("/a/b/c", instance.get(interest).getName().toUri());
  }

  @Test(expected = DataNotFoundException.class)
  public void testFailure() throws DataNotFoundException {
    instance.get(new Interest(new Name("/unknown/data")));
  }

  @Test(expected = DataNotFoundException.class)
  public void testCleanup() throws DataNotFoundException, InterruptedException {
    instance.put(RepoHelper.buildAlmostStaleData("/stale/data"));
    instance.put(RepoHelper.buildFreshData("/fresh/data"));

    Thread.sleep(10);
    instance.cleanup();

    assertNotNull(instance.get(buildInterest("/fresh/data")));
    instance.get(buildInterest("/stale/data"));
  }

  @Test(expected = DataNotFoundException.class)
  public void testFreshnessFlag() throws InterruptedException, DataNotFoundException {
    instance.put(buildAlmostStaleData("/stale/data"));
    Thread.sleep(10);
    Interest interest = buildInterest("/stale/data");
    interest.setMustBeFresh(true);
    instance.get(interest);
  }

  @Test
  public void testSatisfies() throws InterruptedException {
    instance.put(RepoHelper.buildAlmostStaleData("/stale/data"));
    instance.put(RepoHelper.buildFreshData("/fresh/data"));

    Thread.sleep(10);

    assertTrue(instance.satisfies(buildInterest("/fresh/data")));
    assertFalse(instance.satisfies(buildInterest("/stale/data")));
    assertFalse(instance.satisfies(buildInterest("/not/found/data")));
  }

  @Test
  public void testChildSelectors() throws DataNotFoundException {
    instance.put(RepoHelper.buildFreshData("/a/a"));
    instance.put(RepoHelper.buildFreshData("/a/b/c/1"));
    instance.put(RepoHelper.buildFreshData("/a/b/c/2"));
    instance.put(RepoHelper.buildFreshData("/a/b/c/3"));
    
    assertTrue(instance.satisfies(buildInterest("/a")));
    
    Data out = instance.get(buildInterest("/a").setChildSelector(Interest.CHILD_SELECTOR_RIGHT));
    assertEquals("/a/b/c/1", out.getName().toUri());
    // you may think this should be /a/b/c/3, but the child selectors only
    // operate on the first component after the interest name; in this case, "b"
  }
}
