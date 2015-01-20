/*
 * File channel: Publisher.java
 * 
 * Purpose: Provide the publisher side of a pub-sub API.
 * 
 * © Copyright Intel Corporation. All rights reserved.
 * Intel Corporation, 2200 Mission College Boulevard,
 * Santa Clara, CA 95052-8119, USA
 */
package com.intel.jndn.utils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.ForwardingFlags;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnInterest;
import net.named_data.jndn.OnRegisterFailed;
import net.named_data.jndn.security.SecurityException;
import net.named_data.jndn.transport.Transport;
import net.named_data.jndn.util.Blob;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This implementation requires both ends (publisher and subscriber) to be
 * routable to each other. When this class receives data to publish (through
 * publish()), it sends an alert to all subscribers indicating new data is
 * available to retrieve; subscribers then retrieve this data using the channel
 * provided in the alert packet. The flow is as follows: - Publisher listens on
 * a given channel (e.g. /my/channel) - Subscriber also listens on a given
 * channel (e.g. /my/channel) - Publisher receives a new Foo to publish -
 * Publisher notifies subscribers of new data (e.g.
 * /my/channel/ALERT/{encoded:/my/channel/foo/[timestamp]} - Subscriber
 * retrieves data (e.g. /my/channel/foo/[timestamp])
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class Publisher implements OnInterest, OnRegisterFailed {

  public static final long DEFAULT_TIMEOUT = 2000;
  private static final Logger logger = LogManager.getLogger();
  protected HashMap<Class, String> types = new HashMap<>();
  protected HashMap<Name, PublishedObject> published = new HashMap<>();
  protected Face face;
  protected Name channel;
  protected boolean registered = true;
  protected long publishLifetime = -1; // in milliseconds, -1 will persist as long as this instance lives

  /**
   * Initialize a publisher on the given channel; published objects will persist
   * as long as this instance lives.
   *
   * @param face
   * @param channel
   * @param registerOnForwarder set this to false to disable interaction with the forwarder (e.g. as in tests)
   */
  public Publisher(final Face face, final Name channel, boolean registerOnForwarder) {
    this.face = face;
    this.channel = channel;
    if (registerOnForwarder) {
      try {
        this.face.registerPrefix(this.channel, this, this, new ForwardingFlags());
      } catch (IOException e) {
        logger.error("Failed to send prefix registration for: " + this.channel.toUri(), e);
      } catch (SecurityException e) {
        logger.error("Failed to configure security correctly for registration: " + this.channel.toUri(), e);
      }
    }
  }

  /**
   * Initialize a publisher on the given channel; published objects will persist
   * as long as this instance lives.
   *
   * @param face
   * @param channel
   */
  public Publisher(final Face face, final Name channel) {
    this(face, channel, true);
  }

  /**
   * Initialize a publisher on the given channel; limit the lifetime of
   * published objects to save memory.
   *
   * @param face
   * @param channel
   * @param publishLifetime
   */
  public Publisher(final Face face, final Name channel, long publishLifetime) {
    this(face, channel);
    this.publishLifetime = publishLifetime;
  }

  /**
   * Add a type and its alias to this publisher
   *
   * @param typeAlias
   * @param type
   */
  public void addType(String typeAlias, Class type) {
    types.put(type, typeAlias);
  }

  /**
   * Publish the given object by sending an alert to all subscribers
   *
   * @param publishedObject
   */
  public void publish(Object publishedObject) {
    // check if this publisher is registered on the face
    if (!registered) {
      logger.error("Publisher failed to register; cannot publish data to: " + channel.toUri());
      return;
    }

    // check known types
    if (!types.containsKey(publishedObject.getClass())) {
      logger.error("Attempted (and failed) to publish unknown type: " + publishedObject.getClass().getName());
      return;
    }

    // check if old published objects need to be removed
    clearOldPublished();

    // track published objects
    Name publishedName = new Name(channel).append(types.get(publishedObject.getClass())).appendTimestamp(System.currentTimeMillis());
    published.put(publishedName, new PublishedObject(publishedObject));

    // send out alert and don't wait for response
    Client client = new Client();
    client.get(face, getAlert(publishedName));
    logger.debug("Notified channel of new published object: " + publishedName.toUri());
  }

  /**
   * Handle incoming requests for published objects; will check the published
   * list and respond with objects that exactly match the incoming interest.
   * TODO signing, encryption
   *
   * @param prefix
   * @param interest
   * @param transport
   * @param registeredPrefixId
   */
  @Override
  public void onInterest(Name prefix, Interest interest, Transport transport, long registeredPrefixId) {
    // check for old published objects
    clearOldPublished();

    // publish object if available
    if (published.containsKey(interest.getName())) {
      try {
        Data data = new Data(interest.getName());
        data.setContent(new Blob("TODO parse object"));
        if (publishLifetime != -1) {
          data.getMetaInfo().setFreshnessPeriod(publishLifetime);
        }
        // data.setSignature(...);
        transport.send(data.wireEncode().signedBuf());
        logger.debug("Sent data: " + interest.getName().toUri());
      } catch (IOException e) {
        logger.error("Failed to send data: " + interest.getName().toUri());
      }
    }
  }

  /**
   * Handle registration failure; this will stop the publisher from sending
   * notifications since the routes will not be setup to respond
   *
   * @param prefix
   */
  @Override
  public void onRegisterFailed(Name prefix) {
    logger.error("Failed to register publisher: " + channel.toUri());
    registered = false;
  }

  /**
   * Remove any published objects that have outlived their lifetime; a lifetime
   * of -1 persists them forever.
   */
  public void clearOldPublished() {
    if (publishLifetime == -1) {
      return;
    }
    for (Entry<Name, PublishedObject> e : published.entrySet()) {
      if (System.currentTimeMillis() - e.getValue().publishedOn > publishLifetime) {
        published.remove(e.getKey());
        logger.debug("Removing old published object: " + e.getKey().toUri());
      }
    }
  }

  /**
   * Build an alert Interest to notify subscribers to retrieve data from this
   * publisher; the Interest will have the form
   * /this/published/channel/ALERT/[encoded channel to request]
   *
   * @param nameToPublish
   * @return
   */
  protected Interest getAlert(Name nameToPublish) {
    Name alertName = new Name(channel).append("ALERT").append(nameToPublish.wireEncode());
    Interest alert = new Interest(alertName);
    alert.setMustBeFresh(true); // interest must reach the application subscribers
    alert.setInterestLifetimeMilliseconds(DEFAULT_TIMEOUT);
    return alert;
  }

  /**
   * Helper class to track published objects and their timestamps
   */
  private class PublishedObject {

    Object publishedObject;
    long publishedOn;

    public PublishedObject(Object object) {
      publishedObject = object;
    }
  }
}
