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

import net.named_data.jndn.Face;
import net.named_data.jndn.Name;
import net.named_data.jndn.util.Blob;

/**
 * @author Andrew Brown, andrew.brown@intel.com
 */
class BlobContentStore implements ContentStore<Blob> {
  @Override
  public void put(Name name, Blob data) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean has(Name name) {
    return false;
  }

  @Override
  public Blob get(Name name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void push(Face face, Name name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException();
  }
}
