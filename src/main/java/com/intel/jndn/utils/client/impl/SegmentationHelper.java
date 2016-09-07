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
package com.intel.jndn.utils.client.impl;

import net.named_data.jndn.Name;
import net.named_data.jndn.encoding.EncodingException;

/**
 * Helper methods for dealing with segmented packets (see
 * http://named-data.net/doc/tech-memos/naming-conventions.pdf).
 *
 * @author Andrew Brown, andrew.brown@intel.com
 */
public class SegmentationHelper {

  /**
   * Determine if a name is segmented, i.e. if it ends with the correct marker
   * type.
   *
   * @param name the name of a packet
   * @param marker the marker type (the initial byte of the component)
   * @return true if the name is segmented
   */
  public static boolean isSegmented(Name name, byte marker) {
    return name.size() > 0 && name.get(-1).getValue().buf().get(0) == marker;
  }

  /**
   * Retrieve the segment number from the last component of a name.
   *
   * @param name the name of a packet
   * @param marker the marker type (the initial byte of the component)
   * @return the segment number
   * @throws EncodingException if the name does not have a final component of
   * the correct marker type
   */
  public static long parseSegment(Name name, byte marker) throws EncodingException {
    if (name.size() == 0) {
      throw new EncodingException("No components to parse.");
    }
    return name.get(-1).toNumberWithMarker(marker);
  }

  /**
   * Remove a segment component from the end of a name
   *
   * @param name the name of a packet
   * @param marker the marker type (the initial byte of the component)
   * @return the new name with the segment component removed or a copy of the
   * name if no segment component was present
   */
  public static Name removeSegment(Name name, byte marker) {
    return isSegmented(name, marker) ? name.getPrefix(-1) : new Name(name);
  }
}
