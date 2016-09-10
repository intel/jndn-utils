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
 * @author Andrew Brown, andrew.brown@intel.com
 */
public interface NameTree<T> {
  Optional<T> content();

  Name.Component lastComponent();

  Name fullName();

  Collection<NameTree<T>> children();

  NameTree<T> parent();

  NameTree<T> insert(Name name, T content);

  Optional<NameTree<T>> find(Name query);

  Optional<NameTree<T>> delete(Name name);

  int count();

  void clear();
}
