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

import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.util.Blob;

import java.io.IOException;
import java.util.Optional;

/**
 * Represents a named content store of NDN data which allows querying by name or interest; TODO merge with Repository
 *
 * @author Andrew Brown, andrew.brown@intel.com
 */
public interface ContentStore {
  /**
   * Store some content under a name
   *
   * @param name the name of the content
   * @param content the bytes of data
   */
  void put(Name name, Blob content);

  /**
   * Check if the content exists
   *
   * @param name the name of the content
   * @return true if the content exists
   */
  boolean has(Name name);

  /**
   * Check if the content exists
   *
   * @param interest a selector-enabled interest to search for content in the store
   * @return true if the content exists
   */
  boolean has(Interest interest);

  /**
   * Retrieve the content by name
   *
   * @param name the name of the content
   * @return the content if it exists
   */
  Optional<Blob> get(Name name);

  /**
   * Retrieve the content by name
   *
   * @param interest the name of the content
   * @return the content if it exists
   */
  Optional<Blob> get(Interest interest);

  /**
   * Write the stored content to the face as Data packets. If no content exists for the given name, this method should
   * have no effect.
   *
   * @param face the face to write to
   * @param name the name of the data to write
   * @throws IOException if the writing fails
   */
  void push(Face face, Name name) throws IOException;

  /**
   * Write the stored content to the face as Data packets. If no content exists for the given name, this method should
   * have no effect.
   *
   * @param face the face to write to
   * @param interest the interest identifying of the data to write
   * @throws IOException if the writing fails
   */
  void push(Face face, Interest interest) throws IOException;

  /**
   * Remove all stored content
   */
  void clear();
}
