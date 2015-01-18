/*
 * File name: ClientObservable.java
 * 
 * Purpose: 
 * 
 * © Copyright Intel Corporation. All rights reserved.
 * Intel Corporation, 2200 Mission College Boulevard,
 * Santa Clara, CA 95052-8119, USA
 */
package com.intel.jndn.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnInterest;
import net.named_data.jndn.OnTimeout;
import net.named_data.jndn.transport.Transport;

/**
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class ClientObservable extends Observable implements OnData, OnTimeout, OnInterest {

	protected List<ClientObservableEvent> events = new ArrayList<>();
	protected List<Interest> incomingInterestPackets = new ArrayList<>();
	protected List<Data> incomingDataPackets;

	@Override
	public void onData(Interest interest, Data data) {
		notifyObservers(new ClientObservableEvent(data));
	}
	
	public void onError(Exception e){
		notifyObservers(new ClientObservableEvent(e));
	}

	@Override
	public void onTimeout(Interest interest) {
		notifyObservers(new ClientObservableEvent());
	}
	
	@Override
	public void onInterest(Name prefix, Interest interest, Transport transport, long registeredPrefixId) {
		notifyObservers(new ClientObservableEvent(interest));
	}
}
