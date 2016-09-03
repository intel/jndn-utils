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

import java.io.IOException;

/**
 * @author Andrew Brown, andrew.brown@intel.com
 */
public interface AnnouncementService {
  void announceEntrance(long id) throws IOException;
  void announceExit(long id) throws IOException;
  Cancellation discoverExistingAnnouncements(On<Long> onFound, On<Void> onComplete, On<Exception> onError);
  Cancellation observeNewAnnouncements(On<Long> onAdded, On<Long> onRemoved, On<Exception> onError) throws RegistrationFailureException;
}
