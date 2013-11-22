/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.hadoop.store2.support;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;

import org.springframework.data.hadoop.store2.codec.CodecInfo;

/**
 * Base implementation of a {@code Storage} sharing a common functionality among storage formats.
 * 
 * @author Janne Valkealahti
 * 
 */
public abstract class StoreObjectSupport extends LifecycleObjectSupport {

	/** Hadoop configuration */
	private final Configuration configuration;

	/** Codec info for storage */
	private final CodecInfo codecInfo;

	/** Hdfs path into a storage */
	private final Path basePath;

	/**
	 * Instantiates a new abstract storage format.
	 * 
	 * @param configuration the hadoop configuration
	 * @param basePath the hdfs path
	 * @param codec the compression codec info
	 */
	public StoreObjectSupport(Configuration configuration, Path basePath, CodecInfo codec) {
		this.configuration = configuration;
		this.basePath = basePath;
		this.codecInfo = codec;
	}

	/**
	 * Gets the configuration.
	 * 
	 * @return the configuration
	 */
	public Configuration getConfiguration() {
		return configuration;
	}

	/**
	 * Gets the path.
	 * 
	 * @return the path
	 */
	public Path getPath() {
		return basePath;
	}

	/**
	 * Gets the codec.
	 * 
	 * @return the codec
	 */
	public CodecInfo getCodec() {
		return codecInfo;
	}

	/**
	 * Checks if is compressed.
	 * 
	 * @return true, if is compressed
	 */
	public boolean isCompressed() {
		return codecInfo != null;
	}

	/**
	 * Gets the resolved path.
	 * 
	 * @return the resolved path
	 */
	protected Path getResolvedPath() {
		return getPath();
	}

}
