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

package org.springframework.data.hadoop.store.support;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;

import org.springframework.data.hadoop.store.Storage;
import org.springframework.util.Assert;

/**
 * Base support class for {@code DataWriter} and {@code DataReader} implementations. What we keep in this class is all
 * the usual shared variables and Spring context stuff.
 * 
 * @author Janne Valkealahti
 * 
 */
public abstract class DataObjectSupport {

	private final Storage storage;

	private final Configuration configuration;

	private final Path path;

	/**
	 * Instantiates a new data object support.
	 * 
	 * @param storage the storage
	 * @param configuration the configuration
	 * @param path the path
	 */
	protected DataObjectSupport(Storage storage, Configuration configuration, Path path) {
		Assert.notNull(storage, "Storage must be set");
		Assert.notNull(configuration, "Configuration must be set");
		Assert.notNull(path, "Path must be set");
		this.storage = storage;
		this.configuration = configuration;
		this.path = path;
	}

	/**
	 * Gets the storage.
	 * 
	 * @return the storage
	 */
	public Storage getStorage() {
		return storage;
	}

	/**
	 * Gets the hadoop configuration.
	 * 
	 * @return the hadoop configuration
	 */
	public Configuration getConfiguration() {
		return configuration;
	}

	/**
	 * Gets the hdfs path.
	 * 
	 * @return the hdfs path
	 */
	public Path getPath() {
		return path;
	}

}
