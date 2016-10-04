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
package com.intel.jndn.utils.processing.impl;

import com.intel.jndn.utils.ProcessingStage;
import com.intel.jndn.utils.ProcessingStageException;
import net.named_data.jndn.Data;
import net.named_data.jndn.util.Blob;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

/**
 * Sample stage for compressing {@link Data} content using GZIP
 *
 * @author Andrew Brown, andrew.brown@intel.com
 */
public class CompressionStage implements ProcessingStage<Data, Data> {

  /**
   * Compress and replace the {@link Data} content. Note: this stage will return
   * the same {@link Data} instance and will modify only its content.
   *
   * @param context the {@link Data} packet
   * @return the same packet but with GZIP-compressed content
   * @throws ProcessingStageException if compression fails
   */
  @Override
  public Data process(Data context) throws ProcessingStageException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    try (GZIPOutputStream stream = new GZIPOutputStream(buffer)) {
      stream.write(context.getContent().getImmutableArray(), 0, context.getContent().size());
      stream.close();
    } catch (IOException e) {
      throw new ProcessingStageException(e);
    }

    context.setContent(new Blob(buffer.toByteArray()));
    return context;
  }

}
