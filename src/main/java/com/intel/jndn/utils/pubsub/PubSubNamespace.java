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

  static final int MESSAGE_MARKER = 100;

  static final int MESSAGE_ATTRIBUTES_MARKER = 101;
  static final int MESSAGE_CONTENT_MARKER = 102;

  static final Name DEFAULT_BROADCAST_PREFIX = new Name("/ndn/broadcast");

  private PubSubNamespace() {
    // do not instantiate this class
  }

  static Name.Component toPublisherComponent(long publisherId) {
    return Name.Component.fromNumberWithMarker(publisherId, PubSubNamespace.PUBLISHER_ID_MARKER);
  }

  static Name toPublisherName(Name prefix, long publisherId) {
    return new Name(prefix).append(toPublisherComponent(publisherId));
  }

  static Name toAnnouncement(Name prefix, long publisherId, Announcement announcement) {
    Name.Component announcementComponent = Name.Component.fromNumberWithMarker(announcement.id, PubSubNamespace.ANNOUNCEMENT_ACTION_MARKER);
    return toPublisherName(prefix, publisherId).append(announcementComponent);
  }

  static Name toMessageName(Name prefix, long publisherId, long messageId) {
    Name.Component messageComponent = Name.Component.fromNumberWithMarker(messageId, PubSubNamespace.MESSAGE_ID_MARKER);
    return toPublisherName(prefix, publisherId).append(messageComponent);
  }

  static Response toResponse(Data data) throws EncodingException {
    long messageId = findMarkerFromEnd(data.getName(), MESSAGE_ID_MARKER);
    TlvDecoder decoder = new TlvDecoder(data.getContent().buf());
    int endOffset = decoder.readNestedTlvsStart(MESSAGE_MARKER);
    Blob attributes = new Blob(decoder.readOptionalBlobTlv(MESSAGE_ATTRIBUTES_MARKER, endOffset), true);
    Blob content = new Blob(decoder.readBlobTlv(MESSAGE_CONTENT_MARKER), true);
    return new Response(data.getName(), messageId, Collections.singletonMap("*", attributes), content);
  }

  static long parsePublisher(Name name) throws EncodingException {
    return findMarkerFromEnd(name, PUBLISHER_ID_MARKER);
  }

  static Announcement parseAnnouncement(Name name) throws EncodingException {
    return Announcement.from((int) name.get(-1).toNumberWithMarker(ANNOUNCEMENT_ACTION_MARKER));
  }

  private static long findMarkerFromEnd(Name name, int marker) throws EncodingException {
    for (int i = name.size() - 1; i >= 0; i--) {
      try {
        return name.get(i).toNumberWithMarker(marker);
      } catch (EncodingException e) {
        // do nothing
      }
    }
    throw new EncodingException("Failed to find marker " + marker + " in name: " + name.toUri());
  }

  enum Announcement {
    ENTRANCE(0),
    EXIT(1);

    private final int id;

    Announcement(int id) {
      this.id = id;
    }

    static Announcement from(int value) {
      for (Announcement a : Announcement.values()) {
        if (a.id == value) return a;
      }
      return null;
    }

    int value() {
      return id;
    }

    public int getValue() {
      return id;
    }
  }
}
