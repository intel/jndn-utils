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

import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.util.Blob;

/**
 * Helper methods for testing.
 *
 * @author Andrew Brown, andrew.brown@intel.com
 */
public class RepoHelper {

  public static Data buildData(String name) {
    Data data = new Data(new Name(name));
    data.setContent(new Blob("..."));
    data.getMetaInfo().setFreshnessPeriod(2000);
    return data;
  }

  public static Interest buildInterest(String name) {
    Interest interest = new Interest(new Name(name));
    interest.setInterestLifetimeMilliseconds(2000);
    return interest;
  }

  public static Data buildAlmostStaleData(String staledata) {
    Data data = RepoHelper.buildData(staledata);
    data.getMetaInfo().setFreshnessPeriod(0);
    return data;
  }

  public static Data buildFreshData(String staledata) {
    Data data = RepoHelper.buildData(staledata);
    data.getMetaInfo().setFreshnessPeriod(-1);
    return data;
  }
}
