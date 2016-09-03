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

import net.named_data.jndn.Data;
import net.named_data.jndn.Name;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.encoding.tlv.TlvDecoder;
import net.named_data.jndn.util.Blob;

import java.util.Collections;

/**
 * @author Andrew Brown, andrew.brown@intel.com
 */
class PubSubNamespace {
  static final int MESSAGE_ID_MARKER = 42;
  static final int PUBLISHER_ID_MARKER = 43;
  static final int ANNOUNCEMENT_ACTION_MARKER = 44;

  static final int ANNOUNCEMENT_ENTRANCE = 1;
  static final int ANNOUNCEMENT_EXIT = 0;

  static final int MESSAGE_MARKER = 100;
  static final int MESSAGE_ATTRIBUTES_MARKER = 101;
  static final int MESSAGE_CONTENT_MARKER = 102;

  static final Name DEFAULT_BROADCAST_PREFIX = new Name("/bcast");

  private PubSubNamespace() {
    // do not instantiate this class
  }

  static Name toPublisherName(Name prefix, long publisherId) {
    Name.Component publisherComponent = Name.Component.fromNumberWithMarker(publisherId, PubSubNamespace.PUBLISHER_ID_MARKER);
    return new Name(prefix).append(publisherComponent);
  }

  static Name toMessageName(Name prefix, long publisherId, long messageId) {
    Name.Component messageComponent = Name.Component.fromNumberWithMarker(messageId, PubSubNamespace.MESSAGE_ID_MARKER);
    return toPublisherName(prefix, publisherId).append(messageComponent);
  }

  static Response toResponse(Data data) throws EncodingException {
    long messageId = extractMessageId(data.getName());
    TlvDecoder decoder = new TlvDecoder(data.getContent().buf());
    int endOffset = decoder.readNestedTlvsStart(MESSAGE_MARKER);
    Blob attributes = new Blob(decoder.readOptionalBlobTlv(MESSAGE_ATTRIBUTES_MARKER, endOffset), true);
    Blob content = new Blob(decoder.readBlobTlv(MESSAGE_CONTENT_MARKER), true);
    return new Response(data.getName(), messageId, Collections.singletonMap("*", attributes), content);
  }

  private static long extractMessageId(Name name) throws EncodingException {
    for (int i = name.size(); i >= 0; i--) {
      try {
        return name.get(i).toNumberWithMarker(MESSAGE_ID_MARKER);
      } catch (EncodingException e) {
        // do nothing
      }
    }
    throw new EncodingException("Failed to find message ID marker in name: " + name.toUri());
  }
}
