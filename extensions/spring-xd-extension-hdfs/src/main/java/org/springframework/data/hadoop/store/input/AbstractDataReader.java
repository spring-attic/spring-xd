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

package org.springframework.data.hadoop.store.input;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;

import org.springframework.data.hadoop.store.DataReader;
import org.springframework.data.hadoop.store.Storage;
import org.springframework.data.hadoop.store.support.DataObjectSupport;

/**
 * Base {@code DataReader} implementation sharing common functionality.
 * 
 * @param <E> Type of an entity for the reader
 * @author Janne Valkealahti
 * 
 */
public abstract class AbstractDataReader<E> extends DataObjectSupport implements DataReader<E> {

	/**
	 * Instantiates a new abstract data reader.
	 * 
	 * @param storage the storage
	 * @param configuration the configuration
	 * @param path the path
	 */
	protected AbstractDataReader(Storage storage, Configuration configuration, Path path) {
		super(storage, configuration, path);
	}

	@Override
	public void open() throws IOException {
		// default impl is no-opt
	}

}
