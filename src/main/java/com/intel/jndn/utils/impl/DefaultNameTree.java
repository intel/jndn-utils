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

import com.intel.jndn.utils.NameTree;
import net.named_data.jndn.Name;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * TODO need a way to bound the size
 *
 * @author Andrew Brown, andrew.brown@intel.com
 */
public class DefaultNameTree<T> implements NameTree<T> {
  private final DefaultNameTree<T> parent;
  private final Map<Name.Component, DefaultNameTree<T>> children = new HashMap<>();
  private Name.Component component;
  private T content;

  DefaultNameTree(DefaultNameTree<T> parent) {
    this.parent = parent;
  }

  public static <T> NameTree<T> newRootTree() {
    return new DefaultNameTree<>(null);
  }

  @Override
  public Optional<T> content() {
    return Optional.ofNullable(content);
  }

  @Override
  public Name fullName() {
    ArrayList<Name.Component> components = new ArrayList<>();
    NameTree<T> self = this;
    while (self.lastComponent() != null) {
      components.add(self.lastComponent());
      self = self.parent();
    }
    Collections.reverse(components);
    return new Name(components);
  }

  @Override
  public Name.Component lastComponent() {
    return component;
  }

  @Override
  public Collection<NameTree<T>> children() {
    List<NameTree<T>> c = new ArrayList<>(children.size());
    for (NameTree<T> nt : children.values()) {
      c.add(nt);
    }
    return c;
  }

  @Override
  public NameTree<T> parent() {
    return parent;
  }

  @Override
  public Optional<NameTree<T>> find(Name name) {
    if (name.size() == 0) {
      return Optional.of(this);
    } else {
      Name.Component first = name.get(0);
      DefaultNameTree<T> child = children.get(first);
      if (child == null) {
        return Optional.empty();
      } else {
        return child.find(name.size() > 1 ? name.getSubName(1) : new Name());
      }
    }
  }

  @Override
  public NameTree<T> insert(Name name, T content) {
    Name.Component first = name.get(0);

    DefaultNameTree<T> child = children.get(first);
    if (child == null) {
      child = new DefaultNameTree<>(this);
      child.component = first;
      children.put(first, child);
    }

    if (name.size() == 1) {
      child.content = content;
      return child;
    } else {
      return child.insert(name.getSubName(1), content);
    }
  }

  @Override
  public Optional<NameTree<T>> delete(Name name) {
    if (name.size() == 0) {
      return Optional.of(parent.deleteChild(this.component));
    } else {
      Name.Component first = name.get(0);
      DefaultNameTree<T> child = children.get(first);
      if (child == null) {
        return Optional.empty();
      } else {
        if (children.size() == 1) {
          children.remove(first);
        }
        return child.delete(name.getSubName(1));
      }
    }
  }

  private NameTree<T> deleteChild(Name.Component component) {
    return children.remove(component);
  }

  @Override
  public int count() {
    throw new UnsupportedOperationException("Breadth-first search?");
  }

  @Override
  public void clear() {
    children.clear();
  }

  @Override
  public String toString() {
    String c = (component == null) ? null : component.toEscapedString();
    return "DefaultNameTree{" + c + ": " + content + '}';
  }
}
