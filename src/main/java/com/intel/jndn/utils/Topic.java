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

import com.intel.jndn.utils.On;
import com.intel.jndn.utils.Publisher;
import com.intel.jndn.utils.Subscriber;
import com.intel.jndn.utils.pubsub.PubSubFactory;
import net.named_data.jndn.Face;
import net.named_data.jndn.Name;
import net.named_data.jndn.util.Blob;

import java.io.IOException;

/**
 * A publish-subscrib topic; see the {@code pubsub} package for implementation details
 *
 * @author Andrew Brown, andrew.brown@intel.com
 */
public final class Topic {
  private final Name name;

  /**
   * @param name the NDN name of the topic
   */
  public Topic(Name name) {
    this.name = name;
  }

  /**
   * @return the NDN name of the topic; the return is wrapped so that the original value is immutable and the return
   * type can be modified
   */
  public Name name() {
    return new Name(name);
  }

  /**
   * @param face the face to use for network IO; must be driven externally (e.g. {@link Face#processEvents()})
   * @param onMessage callback fired when a message is received
   * @param onError callback fired when an error happens after subscription
   * @return an open subscriber
   * @throws IOException if the subscription fails
   */
  public Subscriber subscribe(Face face, On<Blob> onMessage, On<Exception> onError) throws IOException {
    return PubSubFactory.newSubscriber(face, name, onMessage, onError);
  }

  /**
   * @param face the face to use for network IO; must be driven externally (e.g. {@link Face#processEvents()})
   * @return a factory-built publisher
   */
  public Publisher newPublisher(Face face) {
    return PubSubFactory.newPublisher(face, name);
  }
}
