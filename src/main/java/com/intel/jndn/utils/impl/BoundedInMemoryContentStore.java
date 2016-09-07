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

import com.intel.jndn.utils.ContentStore;
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Name;
import net.named_data.jndn.util.Blob;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * @author Andrew Brown, andrew.brown@intel.com
 */
public class BoundedInMemoryContentStore implements ContentStore {
  private final BoundedLinkedMap<Name, Blob> store;
  private final Data template;

  public BoundedInMemoryContentStore(int maxSize, int freshnessMs) {
    this.store = new BoundedLinkedMap<>(maxSize);
    this.template = new Data();
    this.template.getMetaInfo().setFreshnessPeriod(freshnessMs);
  }

  @Override
  public void put(Name name, Blob data) {
    store.put(name, data);
  }

  @Override
  public boolean has(Name name) {
    return store.containsKey(name);
  }

  @Override
  public Blob get(Name name) {
    return store.get(name);
  }

  @Override
  public void push(Face face, Name name) throws IOException {
    Blob blob = get(name);
    if (blob != null) {
      Data t = new Data(template);
      t.setName(name);
      ByteArrayInputStream b = new ByteArrayInputStream(blob.getImmutableArray());
      for (Data d : SegmentationHelper.segment(t, b)) {
        face.putData(d);
      }
    }
  }

  @Override
  public void clear() {
    store.clear();
  }
}
