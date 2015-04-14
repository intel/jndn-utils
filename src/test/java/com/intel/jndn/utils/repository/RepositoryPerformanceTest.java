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

import static com.intel.jndn.utils.repository.RepoHelper.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Test NDN repositories, including performance
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class RepositoryPerformanceTest {

  private static final Logger logger = Logger.getLogger(RepositoryPerformanceTest.class.getName());

  @Test
  public void testGenerator() {
    List<Name> names = buildNames(100);
    assertEquals(100, names.size());
    assertNotSame(names.get(0), names.get(1));
    assertNotSame(names.get(0), names.get(26));
  }

  /**
   * Some conclusions: the tree is far, far slower to add but far, far faster on
   * retrieval.
   *
   * @throws DataNotFoundException
   */
  @Test
  public void testPerformance() throws DataNotFoundException {
    double seconds = 1000000000.0;
    List<Name> names = buildNames(1000);
    List<Data> datas = buildDatas(names);
    List<Interest> interests = buildInterests(names);

    Repository repo1 = new ForLoopRepository();
    long time1 = timeAddDatas(repo1, datas);
    logger.info("Put in for-loop repo (sec): " + time1 / seconds);

    Repository repo2 = new NavigableTreeRepository();
    long time2 = timeAddDatas(repo2, datas);
    logger.info("Put in tree repo (sec): " + time2 / seconds);

    long time3 = timeFindDatas(repo1, interests);
    logger.info("Get from for-loop repo (sec): " + time3 / seconds);

    long time4 = timeFindDatas(repo2, interests);
    logger.info("Get from tree repo (sec): " + time4 / seconds);

    long time5 = timeFindChildSelectedDatas(repo1, interests);
    logger.info("Get child-selected from for-loop repo (sec): " + time5 / seconds);

    long time6 = timeFindChildSelectedDatas(repo2, interests);
    logger.info("Get child-selected from tree repo (sec): " + time6 / seconds);
  }

  public static List<Name> buildNames(int size) {
    List<Name> names = new ArrayList<>();
    for (Name name : new NameGenerator(size)) {
      names.add(name);
    }
    return names;
  }

  public static List<Data> buildDatas(List<Name> names) {
    List<Data> datas = new ArrayList<>();
    for (Name name : names) {
      datas.add(buildData(name.toUri()));
    }
    return datas;
  }

  public static List<Interest> buildInterests(List<Name> names) {
    List<Interest> interests = new ArrayList<>();
    for (Name name : names) {
      interests.add(buildInterest(name.toUri()));
    }
    return interests;
  }

  public static long timeAddDatas(Repository repo, List<Data> datas) {
    long start = System.nanoTime();
    for (Data data : datas) {
      repo.put(data);
    }
    return System.nanoTime() - start;
  }

  public static long timeFindDatas(Repository repo, List<Interest> interests) throws DataNotFoundException {
    long start = System.nanoTime();
    for (Interest interest : interests) {
      Data data = repo.get(interest);
      assertNotNull(data);
    }
    return System.nanoTime() - start;
  }

  public static long timeFindChildSelectedDatas(Repository repo, List<Interest> interests) throws DataNotFoundException {
    long start = System.nanoTime();
    for (Interest interest : interests) {
      interest.setChildSelector(Interest.CHILD_SELECTOR_RIGHT);
      Data data = repo.get(interest);
      assertNotNull(data);
    }
    return System.nanoTime() - start;
  }


  public static class NameGenerator implements Iterator<Name>, Iterable<Name> {

    private int size;
    private int count = 0;
    private Name last = new Name();

    public NameGenerator(int size) {
      this.size = size;
    }

    @Override
    public boolean hasNext() {
      return count < size;
    }

    @Override
    public Name next() {
      int current = count % 26;
      String letter = Character.toString((char) (current + 65));
      if (current == 0) {
        last.append(letter);
      } else {
        last = last.getPrefix(-1).append(letter);
      }
      count++;
      return new Name(last);
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Iterator<Name> iterator() {
      return this;
    }
  }
}
