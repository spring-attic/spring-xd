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

package org.springframework.data.hadoop.store.output;

import java.nio.ByteBuffer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;

import org.springframework.data.hadoop.store.StrategiesStorage;
import org.springframework.data.hadoop.store.support.DataUtils;


/**
 * A {@code DataWriter} for delimited field data.
 * 
 * @author Janne Valkealahti
 * 
 */
public class DelimitedTextEntityWriter extends AbstractEntityWriter<String[]> {

	/** CSV Mode */
	public final static byte[] CSV = DataUtils.getUTF8CsvDelimiter();

	/** TAB Mode */
	public final static byte[] TAB = DataUtils.getUTF8TabDelimiter();

	/** Field delimiter */
	private final byte[] delimiter;

	/**
	 * Instantiates a new delimited text data writer.
	 * 
	 * @param storage the storage
	 * @param configuration the configuration
	 * @param path the path
	 * @param delimiter the field delimiter
	 */
	public DelimitedTextEntityWriter(StrategiesStorage storage, Configuration configuration, Path path, byte[] delimiter) {
		super(storage, configuration, path);
		this.delimiter = delimiter;
	}

	@Override
	protected byte[] convert(String[] entity) {
		ByteBuffer buf = ByteBuffer.allocate(128);
		for (int i = 0; i < entity.length; i++) {
			buf.put(entity[i].getBytes());
			if (i < (entity.length - 1)) {
				buf.put(delimiter);
			}
		}
		return buf.array();
	}

}
