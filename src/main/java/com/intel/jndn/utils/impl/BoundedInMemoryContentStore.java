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

/**
 * @author Andrew Brown, andrew.brown@intel.com
 */
public class BoundedInMemoryContentStore implements ContentStore {
  private final NameTree<Blob> store;
  private final Data template;

  public BoundedInMemoryContentStore(int maxSize, int freshnessMs) {
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
    Optional<NameTree<Blob>> possibleBlob = store.find(interest.getName());
    if (!possibleBlob.isPresent()) {
      return Optional.empty();
    } else if (hasSelectors(interest)) {
      List<NameTree<Blob>> children = new ArrayList<>(possibleBlob.get().children());
      if (children.isEmpty()) {
        return Optional.empty();
      } else if (children.size() == 1) {
        return children.get(0).content();
      } else {
        if (isRightMost(interest)) {
          Collections.sort(children, (a, b) -> b.lastComponent().compare(a.lastComponent()));
        } else if (isLeftMost(interest)) {
          Collections.sort(children, (a, b) -> a.lastComponent().compare(b.lastComponent()));
        }
        return children.get(0).content();
      }

      // TODO max min suffix components

    } else {
      return possibleBlob.get().content();
    }
  }

  // TODO merge with above
  private Optional<NamePair> getNamePair(Interest interest) {
    Optional<NameTree<Blob>> possibleBlob = store.find(interest.getName());
    if (!possibleBlob.isPresent()) {
      return Optional.empty();
    } else if (hasSelectors(interest)) {
      List<NameTree<Blob>> children = new ArrayList<>(possibleBlob.get().children());
      if (children.isEmpty()) {
        return Optional.empty();
      } else if (children.size() == 1) {
        return NamePair.fromContent(children.get(0));
      } else {
        if (isRightMost(interest)) {
          Collections.sort(children, (a, b) -> b.lastComponent().compare(a.lastComponent()));
        } else if (isLeftMost(interest)) {
          Collections.sort(children, (a, b) -> a.lastComponent().compare(b.lastComponent()));
        }
        return NamePair.fromContent(children.get(0));
      }

      // TODO max min suffix components

    } else {
      return NamePair.fromContent(possibleBlob.get());
    }
  }

  private boolean hasSelectors(Interest interest) {
    return interest.getChildSelector() != -1 || interest.getExclude().size() > 0;
  }

  private boolean isRightMost(Interest interest) {
    return interest.getChildSelector() == Interest.CHILD_SELECTOR_RIGHT;
  }

  private boolean isLeftMost(Interest interest) {
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
    Optional<NamePair> pair = getNamePair(interest);
    if (pair.isPresent()) {
      Data t = new Data(template);
      t.setName(pair.get().name);
      ByteArrayInputStream b = new ByteArrayInputStream(pair.get().blob.getImmutableArray());
      for (Data d : SegmentationHelper.segment(t, b)) {
        face.putData(d);
      }
    }
  }

  @Override
  public void clear() {
    store.clear();
  }

  private static class NamePair {
    public final Name name;
    public final Blob blob;

    public NamePair(Name name, Blob blob) {
      this.name = name;
      this.blob = blob;
    }

    static Optional<NamePair> fromContent(NameTree<Blob> leaf) {
      Optional<Blob> content = leaf.content();
      if (content.isPresent()) {
        return Optional.of(new NamePair(leaf.fullName(), leaf.content().get()));
      } else {
        return Optional.empty();
      }
    }
  }
}
