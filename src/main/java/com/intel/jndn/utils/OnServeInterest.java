/*
 * File name: OnServeInterest.java
 * 
 * Purpose: Functional interface for serving data from Server.on()
 * 
 * © Copyright Intel Corporation. All rights reserved.
 * Intel Corporation, 2200 Mission College Boulevard,
 * Santa Clara, CA 95052-8119, USA
 */
package com.intel.jndn.utils;

import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;

/**
 * Functional interface for serving data from Server.on()
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public interface OnServeInterest {

  public Data onInterest(Name prefix, Interest interest);
}
