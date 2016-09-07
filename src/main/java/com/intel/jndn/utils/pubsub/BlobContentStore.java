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

import com.intel.jndn.utils.ContentStore;
import net.named_data.jndn.Face;
import net.named_data.jndn.Name;
import net.named_data.jndn.util.Blob;

/**
 * @author Andrew Brown, andrew.brown@intel.com
 */
class BlobContentStore implements ContentStore {
  private final BoundedLinkedMap<Name, Blob> store;

  BlobContentStore(int maxSize) {
    this.store = new BoundedLinkedMap<>(maxSize);
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
  public void push(Face face, Name name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clear() {
    store.clear();
  }
}
