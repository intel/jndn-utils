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
package com.intel.jndn.utils.repository;

import com.intel.jndn.utils.repository.Repository;
import com.intel.jndn.utils.repository.DataNotFoundException;
import com.intel.jndn.utils.repository.ForLoopRepository;
import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test {@link ForLoopRepository}.
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class ForLoopRepositoryTest {

  @Test
  public void testGetAndPut() throws DataNotFoundException {
    Repository repo = new ForLoopRepository();
    repo.put(RepositoryTest.buildData("/a/b/c"));
    Data data = repo.get(RepositoryTest.buildInterest("/a/b/c"));
    assertEquals("...", data.getContent().toString());
  }

  @Test
  public void testForLoopChildSelectors() throws DataNotFoundException {
    Repository repo = new ForLoopRepository();
    RepositoryTest.assertChildSelectorsRetrieve(repo);
  }

  @Test(expected = DataNotFoundException.class)
  public void testFailure() throws DataNotFoundException {
    Repository repo = new ForLoopRepository();
    repo.get(new Interest(new Name("/unknown/data")));
  }
}
