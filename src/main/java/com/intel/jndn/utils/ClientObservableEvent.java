/*
 * File name: ClientObservableEvent.java
 * 
 * Purpose: 
 * 
 * © Copyright Intel Corporation. All rights reserved.
 * Intel Corporation, 2200 Mission College Boulevard,
 * Santa Clara, CA 95052-8119, USA
 */
package com.intel.jndn.utils;

/**
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class ClientObservableEvent<T> {

	boolean success;
	long timestamp;
	T packet;

	public ClientObservableEvent(T packet) {
		fromPacket(packet);
	}

	public ClientObservableEvent() {
		timestamp = System.currentTimeMillis();
		success = false;
	}
	
	public final void fromPacket(T packet_){
		timestamp = System.currentTimeMillis();
		success = !Exception.class.isInstance(packet_);
		packet = packet_;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public T getPacket() {
		return packet;
	}

	public void setPacket(T packet) {
		this.packet = packet;
	}

}
