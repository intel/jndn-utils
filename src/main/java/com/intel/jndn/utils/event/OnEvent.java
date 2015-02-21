/*
 * File name: OnEvent.java
 * 
 * Purpose: Interface for registering generic event handlers
 * 
 * © Copyright Intel Corporation. All rights reserved.
 * Intel Corporation, 2200 Mission College Boulevard,
 * Santa Clara, CA 95052-8119, USA
 */
package com.intel.jndn.utils.event;

/**
 * Interface for registering generic event handlers. E.g.: observer.then((event)
 * => doSomething(event.getFirst()));
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public interface OnEvent {

  public void onEvent(NDNEvent event);
}
