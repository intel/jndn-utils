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

import net.named_data.jndn.Name;
import net.named_data.jndn.util.Blob;

import java.util.Map;

/**
 * @author Andrew Brown, andrew.brown@intel.com
 */
class Response {
  private final Name name;
  private final long messageId;
  private final Map<String, Object> attributes;
  private final Blob content;

  Response(Name name, long messageId, Map<String, Object> attributes, Blob content) {
    this.name = name;
    this.messageId = messageId;
    this.attributes = attributes;
    this.content = content;
  }

  long messageId() {
    return messageId;
  }

  Blob content() {
    return content;
  }
}
