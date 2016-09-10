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

import com.intel.jndn.utils.Publisher;
import com.intel.jndn.utils.Subscriber;
import com.intel.jndn.utils.client.impl.AdvancedClient;
import com.intel.jndn.utils.impl.BoundedInMemoryContentStore;
import com.intel.jndn.utils.impl.BoundedInMemoryPendingInterestTable;
import net.named_data.jndn.Face;
import net.named_data.jndn.Name;
import net.named_data.jndn.util.Blob;

import java.io.IOException;
import java.util.Random;

/**
 * @author Andrew Brown, andrew.brown@intel.com
 */
public final class Topic {
  private final Name name;

  public Topic(Name name) {
    this.name = name;
  }

  public Name name() {
    return new Name(name);
  }

  // TODO move to PubSubFactory? change this to subscribe()
  public Subscriber subscribe(Face face, On<Blob> onMessage, On<Exception> onError) throws RegistrationFailureException, IOException {
    NdnSubscriber subscriber = new NdnSubscriber(face, name, onMessage, onError, new NdnAnnouncementService(face, name), new AdvancedClient());
    subscriber.open();
    return subscriber;
  }

  // TODO move to PubSubFactory? change this to publish()
  public Publisher newPublisher(Face face) {
    long publisherId = Math.abs(new Random().nextLong());
    return new NdnPublisher(face, name, publisherId, new NdnAnnouncementService(face, name), new BoundedInMemoryPendingInterestTable(1024), new BoundedInMemoryContentStore(1024, 2000));
  }
}
