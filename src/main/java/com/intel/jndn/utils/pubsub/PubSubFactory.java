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

import com.intel.jndn.utils.On;
import com.intel.jndn.utils.Publisher;
import com.intel.jndn.utils.Subscriber;
import com.intel.jndn.utils.client.impl.AdvancedClient;
import com.intel.jndn.utils.impl.BoundedInMemoryPendingInterestTable;
import com.intel.jndn.utils.impl.InMemoryContentStore;
import net.named_data.jndn.Face;
import net.named_data.jndn.Name;
import net.named_data.jndn.util.Blob;

import java.io.IOException;
import java.security.SecureRandom;

/**
 * Assemble the necessary elements for building the {@link Publisher} and {@link Subscriber} implementations
 *
 * @author Andrew Brown, andrew.brown@intel.com
 */
public class PubSubFactory {

  private PubSubFactory() {
    // do not instantiate this factory
  }

  /**
   * @param face the face to use for network IO; must be driven externally (e.g. {@link Face#processEvents()})
   * @param prefix the NDN namespace under which messages are published
   * @param onMessage callback fired when a message is received
   * @param onError callback fired when an error happens after subscription
   * @return an open, publisher-discovering, message-retrieving subscriber
   * @throws IOException if the initial subscription or announcement IO fails
   */
  public static Subscriber newSubscriber(Face face, Name prefix, On<Blob> onMessage, On<Exception> onError) throws IOException {
    NdnAnnouncementService announcementService = new NdnAnnouncementService(face, prefix);
    AdvancedClient client = new AdvancedClient();
    NdnSubscriber subscriber = new NdnSubscriber(face, prefix, onMessage, onError, announcementService, client);
    subscriber.open();
    return subscriber;
  }

  /**
   * @param face the face to use for network IO; must be driven externally (e.g. {@link Face#processEvents()})
   * @param prefix the NDN namespace under which messages are published
   * @return a group-announcing, unopened subscriber (it will automatically open on first publish)
   */
  public static Publisher newPublisher(Face face, Name prefix) {
    long publisherId = Math.abs(new SecureRandom().nextLong());
    return new NdnPublisher(face, prefix, publisherId, new NdnAnnouncementService(face, prefix), new BoundedInMemoryPendingInterestTable(1024), new InMemoryContentStore(2000));
  }
}
