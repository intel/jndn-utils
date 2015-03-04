/*
 * jndn-utils
 * Copyright (c) 2015, Intel Corporation.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms and conditions of the GNU Lesser General Public License,
 * version 3, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details.
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
