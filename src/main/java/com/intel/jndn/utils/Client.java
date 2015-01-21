/*
 * File name: Client.java
 * 
 * Purpose: Provide a client to simplify information retrieval over the NDN
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
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnInterest;
import net.named_data.jndn.OnRegisterFailed;
import net.named_data.jndn.OnTimeout;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.transport.Transport;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Provide a client to simplify information retrieval over the NDN network.
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class Client {

  public static final long DEFAULT_SLEEP_TIME = 20;
  public static final long DEFAULT_TIMEOUT = 2000;
  private static final Logger logger = LogManager.getLogger();
  private static Client defaultInstance;

  /**
   * Singleton access for simpler client use
   *
   * @return
   */
  public static Client getDefault() {
    if (defaultInstance == null) {
      defaultInstance = new Client();
    }
    return defaultInstance;
  }

  /**
   * Synchronously retrieve the Data for an Interest; this will block until
   * complete (i.e. either data is received or the interest times out).
   *
   * @param face
   * @param interest
   * @return Data packet or null
   */
  public Data getSync(Face face, Interest interest) {
    // setup event
    long startTime = System.currentTimeMillis();
    final ClientEvent event = new ClientEvent(); // this event is used without observer/observables for speed; just serves as a final reference into the callbacks

    // send interest
    try {
      face.expressInterest(interest, new OnData() {
        @Override
        public void onData(Interest interest, Data data) {
          event.fromPacket(data);
        }
      }, new OnTimeout() {
        @Override
        public void onTimeout(Interest interest) {
          event.fromPacket(new Exception("Interest timed out: " + interest.getName().toUri()));
        }
      });
    } catch (IOException e) {
      logger.warn("IO failure while sending interest.", e);
      return null;
    }

    // process eventCount until a response is received or timeout
    while (event.getPacket() == null) {
      try {
        synchronized (face) {
          face.processEvents();
        }
      } catch (IOException | EncodingException e) {
        logger.warn("Failed to process events.", e);
        return null;
      }
      sleep();
    }

    // return
    logger.debug("Request time (ms): " + (event.getTimestamp() - startTime));
    return (event.isSuccess()) ? (Data) event.getPacket() : null;
  }

  /**
   * Synchronously retrieve the Data for a Name using a default interest (e.g. 2
   * second timeout); this will block until complete (i.e. either data is
   * received or the interest times out).
   *
   * @param face
   * @param name
   * @return
   */
  public Data getSync(Face face, Name name) {
    return getSync(face, getDefaultInterest(name));
  }

  /**
   * Asynchronously retrieve the Data for a given interest; use the returned
   * ClientObserver to handle the Data when it arrives. For example (with lambdas):
   * <pre><code>
   * Client.getDefault().get(face, interest).then((event) -> doSomething(event));
   * </code></pre>
   * 
   * If you want to block until the response returns, try something like:
   * <pre><code>
   * ClientObserver observer = Client.getDefault().get(face, interest);
   * while(observer.eventCount() == 0){
   *   Thread.sleep(50);
   * }
   * doSomething(observer.getFirst());
   * </code></pre>
   *
   * @param face
   * @param interest
   * @return
   */
  public ClientObserver get(final Face face, final Interest interest) {
    // setup observer
    final ClientObserver observer = new ClientObserver();
    final ClientObservable eventHandler = new ClientObservable();
    eventHandler.addObserver(observer);

    // setup background thread
    Thread backgroundThread = new Thread(new Runnable() {
      @Override
      public void run() {
        // send interest
        try {
          face.expressInterest(interest, eventHandler, eventHandler);
        } catch (IOException e) {
          logger.warn("IO failure while sending interest.", e);
          eventHandler.notify(e);
        }

        // process eventCount until a response is received or timeout
        while (observer.dataCount() == 0 && observer.errorCount() == 0 && !observer.mustStop()) {
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

        // finished
        logger.trace("Received response; stopping thread.");
      }
    });
    backgroundThread.setName(String.format("Client.get(%s)", interest.getName().toUri()));
    backgroundThread.setDaemon(true);
    backgroundThread.start();

    // return
    return observer;
  }

  /**
   * Asynchronously retrieve the Data for a Name using default Interest
   * parameters; see get(Face, Interest) for examples.
   *
   * @param face
   * @param name
   * @return
   */
  public ClientObserver get(Face face, Name name) {
    return get(face, getDefaultInterest(name));
  }

  /**
   * Synchronously serve a Data on the given face until one request accesses the
   * data; will return incoming Interest request. E.g.: Interest request =
   * Client.putSync(face, data);
   *
   * @param face
   * @param data
   * @return
   */
  public Interest putSync(Face face, final Data data) {
    // setup event
    long startTime = System.currentTimeMillis();
    final String dataName = data.getName().toUri();
    final ClientEvent event = new ClientEvent();

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
   * E.g.: ClientObserver observer = Client.put(face, data); // when finished
   * serving the data, stop the background thread observer.stop();
   *
   * @param face
   * @param data
   * @return
   */
  public ClientObserver put(final Face face, final Data data) {
    // setup observer
    final ClientObserver observer = new ClientObserver();
    final ClientObservable eventHandler = new ClientObservable();
    eventHandler.addObserver(observer);

    // setup handlers
    final OnInterest interestHandler = new OnInterest() {
      @Override
      public void onInterest(Name prefix, Interest interest, Transport transport, long registeredPrefixId) {
        try {
          transport.send(data.wireEncode().buf());
        } catch (IOException e) {
          logger.error("Failed to send data for: " + data.getName().toUri());
          eventHandler.onError(e);
        }
      }
    };
    final OnRegisterFailed failureHandler = new OnRegisterFailed() {
      @Override
      public void onRegisterFailed(Name prefix) {
        logger.error("Failed to register name to put: " + data.getName().toUri());
        eventHandler.onError(new Exception("Failed to register name to put: " + data.getName().toUri()));
      }
    };
    final ForwardingFlags flags = new ForwardingFlags();
    flags.setCapture(true);

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
          eventHandler.onError(e);
        } catch (net.named_data.jndn.security.SecurityException e) {
          logger.error("Error registering prefix: " + data.getName().toUri(), e);
          eventHandler.onError(e);
        }

        // process eventCount until a request is received
        while (observer.interestCount() == 0 && observer.errorCount() == 0 && !observer.mustStop()) {
          try {
            synchronized (face) {
              face.processEvents();
            }
          } catch (IOException | EncodingException e) {
            logger.warn("Failed to process events.", e);
            observer.update(eventHandler, new ClientEvent());
          }
          sleep();
        }
      }
    });
    backgroundThread.setName(String.format("Client.put(%s)", data.getName().toUri()));
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

  /**
   * Create a default interest for a given Name using some common settings: -
   * lifetime: 2 seconds
   *
   * @param name
   * @return
   */
  public Interest getDefaultInterest(Name name) {
    Interest interest = new Interest(name, DEFAULT_TIMEOUT);
    return interest;
  }
}
