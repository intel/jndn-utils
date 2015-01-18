/*
 * File name: ClientObserver.java
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
import java.util.Observer;
import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;
import net.named_data.jndn.OnData;

/**
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class ClientObserver implements Observer {

	protected List<ClientObservableEvent> events = new ArrayList<>();
	protected long timestamp;
	protected OnData then;
	protected boolean stopThread;
	
	public ClientObserver(){
		timestamp = System.currentTimeMillis();
	}

	@Override
	public void update(Observable o, Object arg) {
		ClientObservableEvent event = (ClientObservableEvent) arg;
		events.add(event);
		if(Data.class.isInstance(event.packet)){
			then.onData(null, (Data) event.packet);
		}
	}
	
	public ClientObserver then(OnData handler){
		then = handler;
		return this;
	}

	public int count(Class type) {
		int count = 0;
		for (ClientObservableEvent event : events) {
			if (type.isInstance(event.packet)) {
				count++;
			}
		}
		return count;
	}

	public int requests() {
		return count(Interest.class);
	}

	public int responses() {
		return count(Data.class);
	}
	
	public int errors(){
		return count(Exception.class);
	}
	
	/**
	 * Get time since observer start
	 * @return 
	 */
	public long getTimeSinceStart(){
		if(getLast() != null){
			return getLast().getTimestamp() - timestamp;
		}
		return -1;
	}

	public ClientObservableEvent getFirst() {
		if (events.size() > 0) {
			return events.get(0);
		}
		return null;
	}

	public ClientObservableEvent getLast() {
		if (events.size() > 0) {
			return events.get(events.size() - 1);
		}
		return null;
	}
	
	public void stop(){
		stopThread = true;
	}
	
	public boolean mustStop(){
		return stopThread;
	}
}
