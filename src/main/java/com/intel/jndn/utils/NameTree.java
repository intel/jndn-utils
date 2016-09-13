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

package com.intel.jndn.utils;

import net.named_data.jndn.Name;

import java.util.Collection;
import java.util.Optional;

/**
 * Represent a tree of nodes, branching based on their component values
 *
 * @author Andrew Brown, andrew.brown@intel.com
 */
public interface NameTree<T> {
  /**
   * @return the full name leading from the root of the tree to this node
   */
  Name fullName();

  /**
   * @return the last component identifying this node; e.g. if the {@link #fullName()} is {@code /a/b/c} for this node,
   * this method must return {@code c}
   */
  Name.Component lastComponent();

  /**
   * @return the optional content stored at this location in the tree
   */
  Optional<T> content();

  /**
   * @return the children of this node or an empty collection
   */
  Collection<NameTree<T>> children();

  /**
   * @return the parent of this node; note that calling this on the root node will return {@code null}
   */
  NameTree<T> parent();

  /**
   * @param name the full name leading to the location in the tree; if intervening nodes do not yet exist, they will be
   * created
   * @param content the content to store at this location; this may overwrite previously existing content, much like a
   * {@link java.util.Map}
   * @return a reference to the node inserted
   */
  NameTree<T> insert(Name name, T content);

  /**
   * @param query the name to use as a path through the tree
   * @return an optional node; if there is no node at the end of the query, the {@link Optional} will be empty
   */
  Optional<NameTree<T>> find(Name query);

  /**
   * @param name the name to use as a path through the tree
   * @return the removed node or an empty {@link Optional} if the node was not found
   */
  Optional<NameTree<T>> delete(Name name);

  /**
   * @return the count of all nodes in the tree below this one that are non-empty (e.g. have some content)
   */
  int count();

  /**
   * Remove all nodes beneath this one; will have no effect on a leaf node
   */
  void clear();
}
