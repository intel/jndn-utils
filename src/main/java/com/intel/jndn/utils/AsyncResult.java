/*
 * File name: AsyncRequest.java
 * 
 * Purpose: Helper class for tracking asynchronous Interest requests
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

public class AsyncResult extends Observable {

	public int responses = 0;
	public boolean done = false;
	public boolean success;
	public List<Data> packets = new ArrayList<>();

	/**
	 * Call this when the request has changed
	 */
	public void changed() {
		this.setChanged();
		this.notifyObservers();
		this.clearChanged();
	}
}
