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
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class Client {

  public static final long DEFAULT_SLEEP_TIME = 20;
  public static final long DEFAULT_TIMEOUT = 2000;
  private static final Logger logger = LogManager.getLogger();

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
    final ClientObservableEvent event = new ClientObservableEvent(); // this event is used without observer/observables for speed; just serves as a final reference into the callbacks

    // send interest
    try {
      face.expressInterest(interest, new OnData() {
        @Override
        public void onData(Interest interest, Data data) {
          event.setTimestamp(System.currentTimeMillis());
          event.setSuccess(true);
          event.setPacket(data);
        }
      }, new OnTimeout() {
        @Override
        public void onTimeout(Interest interest) {
          event.setTimestamp(System.currentTimeMillis());
          event.setSuccess(false);
          event.setPacket(new Object());
        }
      });
    } catch (IOException e) {
      logger.warn("IO failure while sending interest.", e);
      return null;
    }

    // process events until a response is received or timeout
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
   * ClientObserver to handle the Data when it arrives. E.g.: Client.get(face,
   * interest).then((data) -> doSomething(data));
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

        // process events until a response is received or timeout
        while (observer.responses() == 0 && observer.errors() == 0 && !observer.mustStop()) {
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
    final ClientObservableEvent event = new ClientObservableEvent();

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

    // process events until one response is sent or error
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

        // process events until a request is received
        while (observer.requests() == 0 && observer.errors() == 0 && !observer.mustStop()) {
          try {
            synchronized (face) {
              face.processEvents();
            }
          } catch (IOException | EncodingException e) {
            logger.warn("Failed to process events.", e);
            observer.update(eventHandler, new ClientObservableEvent());
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
