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
import com.intel.jndn.utils.NameTree;
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.util.Blob;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * TODO must bound the size of the content store
 *
 * @author Andrew Brown, andrew.brown@intel.com
 */
public class InMemoryContentStore implements ContentStore {
  private static final Logger LOGGER = Logger.getLogger(InMemoryContentStore.class.getName());
  private final NameTree<Blob> store;
  private final Data template;

  public InMemoryContentStore(int freshnessMs) {
    this.template = new Data();
    this.template.getMetaInfo().setFreshnessPeriod(freshnessMs);
    this.store = DefaultNameTree.newRootTree();
  }

  @Override
  public void put(Name name, Blob data) {
    store.insert(name, data);
  }

  @Override
  public Optional<Blob> get(Interest interest) {
    Optional<NameTree<Blob>> leaf = getWithSelectors(interest);
    return leaf.isPresent() ? leaf.get().content() : Optional.empty();
  }

  private Optional<NameTree<Blob>> getWithSelectors(Interest interest) {
    Optional<NameTree<Blob>> possibleBlob = store.find(interest.getName());
    if (possibleBlob.isPresent() && hasSelectors(interest)) {
      List<NameTree<Blob>> children = new ArrayList<>(possibleBlob.get().children());
      if (children.isEmpty()) {
        return Optional.empty();
      } else if (children.size() == 1) {
        return Optional.of(children.get(0));
      } else if (isRightMost(interest)) {
        Collections.sort(children, (a, b) -> b.lastComponent().compare(a.lastComponent()));
        return Optional.of(children.get(0));
      } else if (isLeftMost(interest)) {
        Collections.sort(children, (a, b) -> a.lastComponent().compare(b.lastComponent()));
        return Optional.of(children.get(0));
      } else {
        // TODO max/min suffix components and excludes
        LOGGER.warning("The interest requested has selectors not yet implemented; returning empty content");
        return Optional.empty();
      }
    }
    return possibleBlob;
  }

  private static boolean hasSelectors(Interest interest) {
    return interest.getChildSelector() != -1 || interest.getExclude().size() > 0;
  }

  private static boolean isRightMost(Interest interest) {
    return interest.getChildSelector() == Interest.CHILD_SELECTOR_RIGHT;
  }

  private static boolean isLeftMost(Interest interest) {
    return interest.getChildSelector() == Interest.CHILD_SELECTOR_LEFT;
  }

  @Override
  public boolean has(Name name) {
    return store.find(name).isPresent();
  }

  @Override
  public boolean has(Interest interest) {
    return get(interest).isPresent();
  }

  @Override
  public Optional<Blob> get(Name name) {
    Optional<NameTree<Blob>> tree = store.find(name);
    return tree.isPresent() ? tree.get().content() : Optional.empty();
  }

  @Override
  public void push(Face face, Name name) throws IOException {
    Optional<Blob> blob = get(name);
    if (blob.isPresent()) {
      Data t = new Data(template);
      t.setName(name);
      ByteArrayInputStream b = new ByteArrayInputStream(blob.get().getImmutableArray());
      for (Data d : SegmentationHelper.segment(t, b)) {
        face.putData(d);
      }
    }
  }

  @Override
  public void push(Face face, Interest interest) throws IOException {
    Optional<NameTree<Blob>> leaf = getWithSelectors(interest);
    if (leaf.isPresent() && leaf.get().content().isPresent()) {
      Data t = new Data(template);
      t.setName(leaf.get().fullName());
      ByteArrayInputStream b = new ByteArrayInputStream(leaf.get().content().get().getImmutableArray());
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
