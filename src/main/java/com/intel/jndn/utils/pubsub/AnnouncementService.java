/*
 * jndn-utils
 * Copyright (c) 2016, Intel Corporation.
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

package com.intel.jndn.utils.pubsub;

import com.intel.jndn.utils.Cancellation;
import com.intel.jndn.utils.On;

import java.io.IOException;

/**
 * API for entering and exiting a group and tracking the accompanying announcements.
 *
 * @author Andrew Brown, andrew.brown@intel.com
 */
interface AnnouncementService {
  /**
   * Announce entrance into the group
   *
   * @param id the ID of the agent
   * @throws IOException if the announcement fails
   */
  void announceEntrance(long id) throws IOException;

  /**
   * Announce exit from the group
   *
   * @param id the ID of the agent
   * @throws IOException if the announcement fails
   */
  void announceExit(long id) throws IOException;

  /**
   * Discover all previously announced entrances; the implementation is expected to be driven by network IO
   * @param onFound callback fired when a group member is discovered
   * @param onComplete callback fired when all group members have been discovered
   * @param onError callback fired if errors occur during discovery
   * @return a cancellation token for stopping discovery
   * @throws IOException if the network fails during discovery
   */
  Cancellation discoverExistingAnnouncements(On<Long> onFound, On<Void> onComplete, On<Exception> onError) throws IOException;

  /**
   * Observe any new entrances/exits; the implementation is expected to be driven by network IO
   * @param onAdded callback fired when a group member announces its entry into the group, see {@link #announceEntrance(long)}
   * @param onRemoved callback fired when a group member announces its exit from the group, see {@link #announceExit(long)}
   * @param onError callback fired if errors are encountered while processing announcements
   * @return a cancellation token for stopping the observer
   * @throws IOException if the network fails while setting up the observer
   */
  Cancellation observeNewAnnouncements(On<Long> onAdded, On<Long> onRemoved, On<Exception> onError) throws IOException;
}
