/*
 * File name: Subscriber.java
 * 
 * Purpose: Provide the subscriber side of a pub-sub API.
 * 
 * © Copyright Intel Corporation. All rights reserved.
 * Intel Corporation, 2200 Mission College Boulevard,
 * Santa Clara, CA 95052-8119, USA
 */
package com.intel.jndn.utils;

import java.io.IOException;
import java.util.HashMap;
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.ForwardingFlags;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.Name.Component;
import net.named_data.jndn.OnInterest;
import net.named_data.jndn.OnRegisterFailed;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.transport.Transport;
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
public class Subscriber implements OnInterest, OnRegisterFailed {

	public static final long DEFAULT_TIMEOUT = 2000;
	private static final Logger logger = LogManager.getLogger();
	protected HashMap<String, Class> types = new HashMap<>();
	protected HashMap<Class, OnPublish> handlers = new HashMap<>();
	protected Face face;
	protected Name channel;

	/**
	 * Initialize a publisher on the given channel; published objects will
	 * persist as long as this instance lives.
	 *
	 * @param face
	 * @param channel
	 */
	public Subscriber(final Face face, final Name channel) {
		this.face = face;
		this.channel = channel;
		try {
			this.face.registerPrefix(this.channel, this, this, new ForwardingFlags());
		} catch (IOException e) {
			logger.error("Failed to send prefix registration for: " + this.channel.toUri(), e);
		} catch (net.named_data.jndn.security.SecurityException e) {
			logger.error("Failed to configure security correctly for registration: " + this.channel.toUri(), e);
		}
	}

	/**
	 * Add a type and its alias to this publisher
	 *
	 * @param typeAlias
	 * @param type
	 */
	public void addType(String typeAlias, Class type) {
		types.put(typeAlias, type);
	}
	
	
	/**
	 * Functional interface defining action to take upon receipt of a published
	 * object
	 * 
	 * @param <T> 
	 */
	public interface OnPublish<T>{
		public void onPublish(T publishedObject);
	}

	/**
	 * Register a handler for receipt of a certain published type
	 *
	 * @param type
	 * @param handler
	 */
	public void on(Class type, OnPublish handler) {
		handlers.put(type, handler);
	}

	/**
	 * Register a handler for receipt of a certain published type
	 *
	 * @param typeAlias
	 * @param handler
	 */
	public void on(String typeAlias, OnPublish handler) {
		if (types.containsKey(typeAlias)) {
			handlers.put(types.get(typeAlias), handler);
		} else {
			logger.warn("Unrecognized type (no handler registered): " + typeAlias);
		}
	}

	/**
	 * Handle alert notifications for published objects; once the published 
	 * object is parsed, this will pass the object to the handle registered
	 * in on().
	 *
	 * @param prefix
	 * @param interest
	 * @param transport
	 * @param registeredPrefixId
	 */
	@Override
	public void onInterest(Name prefix, Interest interest, Transport transport, long registeredPrefixId) {
		// check format
		if (!isAlert(interest)) {
			logger.warn("Incoming interest was not in ALERT format: " + interest.getName().toUri());
			return;
		}

		// check signature
		// TODO
		// retrieve name
		Name publishedName = getAlertName(interest);
		if (publishedName == null) {
			return;
		}

		// retrieve data
		Client client = new Client();
		Data data = client.getSync(face, publishedName);
		if (data == null) {
			logger.warn("Faled to retrieve published object: " + publishedName.toUri());
			return;
		}

		// get handler
		String typeAlias = getPublishedType(data);
		if (!types.containsKey(typeAlias)) {
			logger.warn("Type not found: " + typeAlias);
			return;
		}
		Class type = types.get(typeAlias);
		if (!handlers.containsKey(type)) {
			logger.warn("No handler found for type: " + typeAlias);
			return;
		}

		// build object
		Object publishedObject = null;
		// TODO parse object

		// call 
		handlers.get(type).onPublish(publishedObject);
	}

	/**
	 * Handle registration failure;
	 *
	 * @param prefix
	 */
	@Override
	public void onRegisterFailed(Name prefix) {
		logger.error("Failed to register: " + prefix.toUri());
	}

	/**
	 * Check if an incoming interest is an alert notification to retrieve data
	 *
	 * @param interest
	 * @return
	 */
	protected boolean isAlert(Interest interest) {
		Component alertComponent = interest.getName().get(-2);
		return alertComponent == null || !alertComponent.equals(new Component("ALERT"));
	}

	/**
	 * Parse an Interest to retrieve the remote name of the published object to
	 * then request; the Interest must have the form
	 * /this/published/channel/ALERT/[encoded name to request]
	 *
	 * @param interest
	 * @return
	 */
	protected Name getAlertName(Interest interest) {
		try {
			Name publishedName = new Name();
			publishedName.wireDecode(interest.getName().get(-1).getValue());
			return publishedName;
		} catch (EncodingException e) {
			logger.error("Failed to parse remote published name", e);
			return null;
		}
	}

	/**
	 * Parse the published type from a Data packet; must be the second to last
	 * component of the name
	 *
	 * @param data
	 * @return
	 */
	protected String getPublishedType(Data data) {
		return data.getName().get(-2).toEscapedString();
	}
}
