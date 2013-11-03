/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.hadoop.store.input;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;

import org.springframework.data.hadoop.store.Storage;
import org.springframework.data.hadoop.store.support.DataUtils;

/**
 * A {@code DataReader} for delimited text data.
 * 
 * @author Janne Valkealahti
 * 
 */
public class DelimitedTextEntityReader extends AbstractEntityReader<String[]> {

	/** CSV Mode */
	public final static byte[] CSV = DataUtils.getUTF8CsvDelimiter();

	/** TAB Mode */
	public final static byte[] TAB = DataUtils.getUTF8TabDelimiter();

	/** Field delimiter */
	private final byte[] delimiter;

	/**
	 * Instantiates a new delimited text entity reader.
	 * 
	 * @param storage the storage
	 * @param configuration the configuration
	 * @param path the path
	 * @param delimiter the delimiter
	 */
	public DelimitedTextEntityReader(Storage storage, Configuration configuration, Path path, byte[] delimiter) {
		super(storage, configuration, path);
		this.delimiter = delimiter;
	}

	@Override
	protected String[] convert(byte[] value) {
		return value != null && value.length > 0 ? new String(value).split(new String(delimiter)) : null;
	}

}
