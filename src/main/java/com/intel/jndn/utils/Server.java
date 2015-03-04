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
package com.intel.jndn.utils;

import com.intel.jndn.utils.event.NDNEvent;
import com.intel.jndn.utils.event.NDNObservable;
import com.intel.jndn.utils.event.NDNObserver;
import java.io.IOException;
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.ForwardingFlags;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnInterest;
import net.named_data.jndn.OnRegisterFailed;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.transport.Transport;
import java.util.logging.Logger;

/**
 * Provide a server to simplify serving data over the NDN network. Exposes two
 * main methods: put() for serving static, known data packets and on() for
 * serving dynamically created packets on-demand.
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class Server {

  public static final long DEFAULT_SLEEP_TIME = 20;
  public static final long DEFAULT_TIMEOUT = 2000;
  private static final Logger logger = Logger.getLogger(Server.class.getName());
  private static Server defaultInstance;
  private KeyChain keyChain;
  private Name certificateName;

  /**
   * Singleton access for simpler server use
   *
   * @return
   */
  public static Server getDefault() {
    if (defaultInstance == null) {
      defaultInstance = new Server();
    }
    return defaultInstance;
  }

  /**
   * Constructor
   */
  public Server() {
    // no signing
  }

  /**
   * Constructor; enables signing
   *
   * @param keyChain
   * @param certificateName
   */
  public Server(KeyChain keyChain, Name certificateName) {
    this.keyChain = keyChain;
    this.certificateName = certificateName;
  }

  /**
   * Synchronously serve a Data on the given face until one request accesses the
   * data; will return incoming Interest request.
   * <pre><code> Interest request = Client.putSync(face, data); </code></pre>
   *
   * @param face
   * @param data
   * @return
   */
  public Interest putSync(Face face, final Data data) {
    // setup event
    long startTime = System.currentTimeMillis();
    final String dataName = data.getName().toUri();
    final NDNEvent event = new NDNEvent();

    // setup flags
    ForwardingFlags flags = new ForwardingFlags();
    flags.setCapture(true);

    // register the data name on the face
    try {
      face.registerPrefix(data.getName(), new OnInterest() {
        @Override
        public void onInterest(Name prefix, Interest interest, Transport transport, long registeredPrefixId) {
          // sign packet
          if (keyChain != null) {
            try {
              keyChain.sign(data, certificateName != null ? certificateName : keyChain.getDefaultCertificateName());
            } catch (net.named_data.jndn.security.SecurityException e) {
              logger.severe("Failed to sign data for: " + dataName + e);
              event.fromPacket(e);
            }
          }

          // send packet
          try {
            transport.send(data.wireEncode().buf());
            logger.fine("Sent data: " + dataName);
            event.fromPacket(interest);
          } catch (IOException e) {
            logger.severe("Failed to send data for: " + dataName);
            event.fromPacket(e);
          }
        }
      }, new OnRegisterFailed() {
        @Override
        public void onRegisterFailed(Name prefix) {
          event.fromPacket(new Exception("Failed to register name: " + dataName));
        }
      }, flags);
      logger.info("Registered data: " + dataName);
    } catch (IOException e) {
      logger.severe("Could not connect to face to register prefix: " + dataName + e);
      event.fromPacket(e);
    } catch (net.named_data.jndn.security.SecurityException e) {
      logger.severe("Error registering prefix: " + dataName + e);
      event.fromPacket(e);
    }

    // process eventCount until one response is sent or error
    while (event.getPacket() == null) {
      try {
        synchronized (face) {
          face.processEvents();
        }
      } catch (IOException | EncodingException e) {
        logger.warning("Failed to process events." + e);
        event.fromPacket(e);
      }
      sleep();
    }

    // return
    logger.fine("Request time (ms): " + (event.getTimestamp() - startTime));
    return (event.isSuccess()) ? (Interest) event.getPacket() : null;
  }

  /**
   * Asynchronously serve a Data on the given face until an observer stops it.
   * E.g.: NDNObserver observer = Client.put(face, data); // when finished
   * serving the data, stop the background thread observer.stop();
   *
   * @param face
   * @param data
   * @return
   */
  public NDNObserver put(final Face face, final Data data) {
    // setup observer
    final NDNObserver observer = new NDNObserver();
    final NDNObservable eventHandler = new NDNObservable();
    eventHandler.addObserver(observer);

    // setup interest handler
    final OnInterest interestHandler = new OnInterest() {
      @Override
      public void onInterest(Name prefix, Interest interest, Transport transport, long registeredPrefixId) {
        // sign packet
        if (keyChain != null) {
          try {
            keyChain.sign(data, certificateName != null ? certificateName : keyChain.getDefaultCertificateName());
          } catch (net.named_data.jndn.security.SecurityException e) {
            logger.severe("Failed to sign data for: " + data.getName().toUri() + e);
            eventHandler.notify(e);
          }
        }

        // send packet
        try {
          transport.send(data.wireEncode().buf());
        } catch (IOException e) {
          logger.severe("Failed to send data for: " + data.getName().toUri());
          eventHandler.notify(e);
        }
      }
    };

    // setup failure handler
    final OnRegisterFailed failureHandler = new OnRegisterFailed() {
      @Override
      public void onRegisterFailed(Name prefix) {
        logger.severe("Failed to register name to put: " + data.getName().toUri());
        eventHandler.notify(new Exception("Failed to register name to put: " + data.getName().toUri()));
      }
    };

    // setup forwarding flags
    final ForwardingFlags flags = new ForwardingFlags();
    flags.setCapture(true); // no shorter routes will answer for this prefix, see http://redmine.named-data.net/projects/nfd/wiki/RibMgmt#Route-inheritance
    flags.setChildInherit(false); // the interest name must be exact, no child components after the prefix

    // start background thread
    Thread backgroundThread = new Thread(new Runnable() {
      @Override
      public void run() {
        // register name on the face
        try {
          face.registerPrefix(data.getName(), interestHandler, failureHandler, flags);
          logger.info("Registered data : " + data.getName().toUri());
        } catch (IOException e) {
          logger.severe("Could not connect to face to register prefix: " + data.getName().toUri() + e);
          eventHandler.notify(e);
        } catch (net.named_data.jndn.security.SecurityException e) {
          logger.severe("Error registering prefix: " + data.getName().toUri() + e);
          eventHandler.notify(e);
        }

        // process eventCount until a request is received
        while (observer.interestCount() == 0 && observer.errorCount() == 0 && !observer.mustStop()) {
          try {
            synchronized (face) {
              face.processEvents();
            }
          } catch (IOException | EncodingException e) {
            logger.warning("Failed to process events." + e);
            eventHandler.notify(e);
          }
          sleep();
        }
      }
    });
    backgroundThread.setName(String.format("Server.put(%s)", data.getName().toUri()));
    backgroundThread.setDaemon(true);
    backgroundThread.start();

    return observer;
  }

  /**
   * Register a prefix on the face to serve Data packets for incoming Interests.
   * This method will create a background thread to process events until the
   * user calls stop() on the returned observer
   *
   * @param face
   * @param prefix
   * @param handler
   * @return
   */
  public NDNObserver on(final Face face, final Name prefix, final OnServeInterest handler) {
    // setup observer
    final NDNObserver observer = new NDNObserver();
    final NDNObservable eventHandler = new NDNObservable();
    eventHandler.addObserver(observer);

    // setup interest handler
    final OnInterest interestHandler = new OnInterest() {
      @Override
      public void onInterest(Name prefix, Interest interest, Transport transport, long registeredPrefixId) {
        // notify observers of interest received
        eventHandler.notify(interest);

        // grab data from OnServeInterest handler
        Data data = handler.onInterest(prefix, interest);

        // sign packet
        if (keyChain != null) {
          try {
            keyChain.sign(data, certificateName != null ? certificateName : keyChain.getDefaultCertificateName());
          } catch (net.named_data.jndn.security.SecurityException e) {
            logger.severe("Failed to sign data for: " + interest.getName().toUri() + e);
            eventHandler.notify(e);
          }
        }

        // send packet
        try {
          transport.send(data.wireEncode().buf());
          eventHandler.notify(data); // notify observers of data sent
        } catch (IOException e) {
          logger.severe("Failed to send data for: " + interest.getName().toUri());
          eventHandler.notify(e);
        }
      }
    };

    // setup failure handler
    final OnRegisterFailed failureHandler = new OnRegisterFailed() {
      @Override
      public void onRegisterFailed(Name prefix) {
        logger.severe("Failed to register name to put: " + prefix.toUri());
        eventHandler.notify(new Exception("Failed to register name to put: " + prefix.toUri()));
      }
    };

    // setup forwarding flags
    final ForwardingFlags flags = new ForwardingFlags();
    flags.setCapture(true); // no shorter routes will answer for this prefix, see http://redmine.named-data.net/projects/nfd/wiki/RibMgmt#Route-inheritance
    flags.setChildInherit(true); // the interest name may have child components after the prefix

    // start background thread
    Thread backgroundThread = new Thread(new Runnable() {
      @Override
      public void run() {
        // register name on the face
        try {
          face.registerPrefix(prefix, interestHandler, failureHandler, flags);
          logger.info("Registered data : " + prefix.toUri());
        } catch (IOException e) {
          logger.severe("Could not connect to face to register prefix: " + prefix.toUri() + e);
          eventHandler.notify(e);
        } catch (net.named_data.jndn.security.SecurityException e) {
          logger.severe("Error registering prefix: " + prefix.toUri() + e);
          eventHandler.notify(e);
        }

        // process events until told to stop or error bubbles up
        while (observer.errorCount() == 0 && !observer.mustStop()) {
          try {
            synchronized (face) {
              face.processEvents();
            }
          } catch (IOException | EncodingException e) {
            logger.warning("Failed to process events." + e);
            eventHandler.notify(e);
          }
          sleep();
        }
      }
    });
    backgroundThread.setName(String.format("Server.on(%s)", prefix.toUri()));
    backgroundThread.setDaemon(true);
    backgroundThread.start();

    return observer;
  }

  /**
   * Put the current thread to sleep to allow time for IO
   */
  protected void sleep() {
    try {
      Thread.currentThread().sleep(DEFAULT_SLEEP_TIME);
    } catch (InterruptedException e) {
      logger.severe("Event loop interrupted." + e);
    }
  }

}
