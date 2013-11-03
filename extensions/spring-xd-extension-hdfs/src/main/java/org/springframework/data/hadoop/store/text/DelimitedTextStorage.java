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

package org.springframework.data.hadoop.store.text;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;

import org.springframework.data.hadoop.store.AbstractStrategiesStorage;
import org.springframework.data.hadoop.store.DataReader;
import org.springframework.data.hadoop.store.DataWriter;
import org.springframework.data.hadoop.store.codec.CodecInfo;
import org.springframework.data.hadoop.store.event.StorageEventPublisher;
import org.springframework.data.hadoop.store.support.DataUtils;

/**
 * A {@code Storage} implementation using a delimited text as a record entry.
 * 
 * @author Janne Valkealahti
 * 
 */
public class DelimitedTextStorage extends AbstractStrategiesStorage {

	/** Data delimiter */
	private final byte[] delimiter;

	/**
	 * Instantiates a new line storage.
	 * 
	 * @param configuration the hadoop configuration
	 * @param path the hdfs path
	 * @param codec the compression codec info
	 */
	public DelimitedTextStorage(Configuration configuration, Path path, CodecInfo codec) {
		super(configuration, path, codec);
		this.delimiter = DataUtils.getUTF8DefaultDelimiter();
	}

	/**
	 * Instantiates a new line storage.
	 * 
	 * @param configuration the configuration
	 * @param path the hdfs path
	 * @param codec the compression codec info
	 */
	public DelimitedTextStorage(Configuration configuration, String path, CodecInfo codec) {
		this(configuration, new Path(path), codec);
	}

	/**
	 * Instantiates a new line storage.
	 * 
	 * @param configuration the hadoop configuration
	 * @param path the hdfs path
	 * @param codec the compression codec info
	 * @param delimiter the line delimiter
	 */
	public DelimitedTextStorage(Configuration configuration, Path path, CodecInfo codec, byte[] delimiter) {
		super(configuration, path, codec);
		this.delimiter = delimiter;
	}

	@Override
	public synchronized DataWriter getDataWriter() throws IOException {
		DelimitedTextDataWriter writer = new DelimitedTextDataWriter(getOutput(), delimiter);
		// TODO: should let spring handle this somehow
		StorageEventPublisher eventPublisher = getStorageEventPublisher();
		if (eventPublisher != null) {
			writer.setStorageEventPublisher(getStorageEventPublisher());
		}
		return writer;
	}

	@Override
	public synchronized DataReader getDataReader(Path path) throws IOException {
		return new DelimitedTextDataReader(getInput(path));
	}

}
