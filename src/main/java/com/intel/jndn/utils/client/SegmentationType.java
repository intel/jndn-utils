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
package com.intel.jndn.utils.client;

/**
 * Documents known partition types from
 * http://named-data.net/doc/tech-memos/naming-conventions.pdf
 *
 * @author Andrew Brown, andrew.brown@intel.com
 */
public enum SegmentationType {

  SEGMENT((byte) 0x00),
  BYTE_OFFSET((byte) 0xFB);

  private final byte marker;

  SegmentationType(byte marker) {
    this.marker = marker;
  }

  public byte value() {
    return marker;
  }
}
