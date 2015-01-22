/*
 * File name: Server.java
 * 
 * Purpose: Provide a server to simplify serving data over the NDN
 * network.
 * 
 * © Copyright Intel Corporation. All rights reserved.
 * Intel Corporation, 2200 Mission College Boulevard,
 * Santa Clara, CA 95052-8119, USA
 */
package com.intel.jndn.utils;

import java.io.IOException;
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.ForwardingFlags;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnInterest;
import net.named_data.jndn.OnRegisterFailed;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.transport.Transport;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
  private static final Logger logger = LogManager.getLogger();
  private static Server defaultInstance;

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
          try {
            transport.send(data.wireEncode().buf());
            logger.debug("Sent data: " + dataName);
            event.fromPacket(interest);
          } catch (IOException e) {
            logger.error("Failed to send data for: " + dataName);
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
      logger.error("Could not connect to face to register prefix: " + dataName, e);
      event.fromPacket(e);
    } catch (net.named_data.jndn.security.SecurityException e) {
      logger.error("Error registering prefix: " + dataName, e);
      event.fromPacket(e);
    }

    // process eventCount until one response is sent or error
    while (event.getPacket() == null) {
      try {
        synchronized (face) {
          face.processEvents();
        }
      } catch (IOException | EncodingException e) {
        logger.warn("Failed to process events.", e);
        event.fromPacket(e);
      }
      sleep();
    }

    // return
    logger.debug("Request time (ms): " + (event.getTimestamp() - startTime));
    return (event.isSuccess()) ? (Interest) event.getPacket() : null;
  }

  /**
   * Asynchronously serve a Data on the given face until an observer stops it.
   * E.g.: NDNObserver observer = Client.put(face, data); // when finished
 serving the data, stop the background thread observer.stop();
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

    // setup handlers
    final OnInterest interestHandler = new OnInterest() {
      @Override
      public void onInterest(Name prefix, Interest interest, Transport transport, long registeredPrefixId) {
        try {
          transport.send(data.wireEncode().buf());
        } catch (IOException e) {
          logger.error("Failed to send data for: " + data.getName().toUri());
          eventHandler.notify(e);
        }
      }
    };
    final OnRegisterFailed failureHandler = new OnRegisterFailed() {
      @Override
      public void onRegisterFailed(Name prefix) {
        logger.error("Failed to register name to put: " + data.getName().toUri());
        eventHandler.notify(new Exception("Failed to register name to put: " + data.getName().toUri()));
      }
    };
    final ForwardingFlags flags = new ForwardingFlags();
    flags.setCapture(true); // no shorter routes will answer for this prefix, see http://redmine.named-data.net/projects/nfd/wiki/RibMgmt#Route-inheritance
    flags.setChildInherit(false); // the interest name must be exact, no child components after the prefix

    // setup background thread
    Thread backgroundThread = new Thread(new Runnable() {
      @Override
      public void run() {
        // register name on the face
        try {
          face.registerPrefix(data.getName(), interestHandler, failureHandler, flags);
          logger.info("Registered data : " + data.getName().toUri());
        } catch (IOException e) {
          logger.error("Could not connect to face to register prefix: " + data.getName().toUri(), e);
          eventHandler.notify(e);
        } catch (net.named_data.jndn.security.SecurityException e) {
          logger.error("Error registering prefix: " + data.getName().toUri(), e);
          eventHandler.notify(e);
        }

        // process eventCount until a request is received
        while (observer.interestCount() == 0 && observer.errorCount() == 0 && !observer.mustStop()) {
          try {
            synchronized (face) {
              face.processEvents();
            }
          } catch (IOException | EncodingException e) {
            logger.warn("Failed to process events.", e);
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
   * This method will create a background thread to process events until 
   * the user calls stop() on the returned observer
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

    // setup handlers
    final OnInterest interestHandler = new OnInterest() {
      @Override
      public void onInterest(Name prefix, Interest interest, Transport transport, long registeredPrefixId) {
        eventHandler.notify(interest);
        try {
          Data data = handler.onInterest(prefix, interest);
          // TODO do signing here?
          transport.send(data.wireEncode().buf());
        } catch (IOException e) {
          logger.error("Failed to send data for: " + prefix.toUri());
          eventHandler.notify(e);
        }
      }
    };
    final OnRegisterFailed failureHandler = new OnRegisterFailed() {
      @Override
      public void onRegisterFailed(Name prefix) {
        logger.error("Failed to register name to put: " + prefix.toUri());
        eventHandler.notify(new Exception("Failed to register name to put: " + prefix.toUri()));
      }
    };
    final ForwardingFlags flags = new ForwardingFlags();
    flags.setCapture(true); // no shorter routes will answer for this prefix, see http://redmine.named-data.net/projects/nfd/wiki/RibMgmt#Route-inheritance
    flags.setChildInherit(true); // the interest name may have child components after the prefix

    // setup background thread
    Thread backgroundThread = new Thread(new Runnable() {
      @Override
      public void run() {
        // register name on the face
        try {
          face.registerPrefix(prefix, interestHandler, failureHandler, flags);
          logger.info("Registered data : " + prefix.toUri());
        } catch (IOException e) {
          logger.error("Could not connect to face to register prefix: " + prefix.toUri(), e);
          eventHandler.notify(e);
        } catch (net.named_data.jndn.security.SecurityException e) {
          logger.error("Error registering prefix: " + prefix.toUri(), e);
          eventHandler.notify(e);
        }

        // process events until told to stop or error bubbles up
        while (observer.errorCount() == 0 && !observer.mustStop()) {
          try {
            synchronized (face) {
              face.processEvents();
            }
          } catch (IOException | EncodingException e) {
            logger.warn("Failed to process events.", e);
            eventHandler.notify(e);
          }
          sleep();
        }
      }
    });
    backgroundThread.setName(String.format("Client.put(%s)", prefix.toUri()));
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
      logger.error("Event loop interrupted.", e);
    }
  }

}
