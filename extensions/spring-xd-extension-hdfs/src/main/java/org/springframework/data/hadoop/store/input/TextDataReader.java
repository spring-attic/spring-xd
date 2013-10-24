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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;

import org.springframework.data.hadoop.store.Storage;
import org.springframework.data.hadoop.store.StorageReader;

/**
 * A {@code DataReader} for text data.
 * 
 * @author Janne Valkealahti
 * 
 */
public class TextDataReader extends AbstractDataReader<String> {

	private final static Log log = LogFactory.getLog(TextDataReader.class);

	/** Flag skipping first entry, needed for splits beyond first one */
	private boolean skipFirst = true;

	/**
	 * Instantiates a new text data reader.
	 * 
	 * @param storage the storage
	 * @param configuration the configuration
	 * @param path the path
	 */
	public TextDataReader(Storage storage, Configuration configuration, Path path) {
		super(storage, configuration, path);
	}

	/**
	 * Instantiates a new text data reader.
	 * 
	 * @param storage the storage
	 * @param configuration the hadoop configuration
	 * @param inputSplit the input split
	 */
	public TextDataReader(Storage storage, Configuration configuration, InputSplit inputSplit) {
		super(storage, configuration, inputSplit);
	}

	@Override
	public String read() throws IOException {
		byte[] bytes = null;
		if (getPath() != null) {
			StorageReader storageReader = getStorage().getStorageReader(getPath());
			bytes = storageReader.read();
		}
		else {
			StorageReader storageReader = getStorage().getStorageReader(getInputSplit());
			if (skipFirst && getInputSplit().getStart() > 0) {
				byte[] skipBytes = storageReader.read();
				if (log.isDebugEnabled()) {
					log.info("skipping first entry: " + new String(skipBytes));
				}
				skipFirst = false;
			}
			bytes = storageReader.read();
		}
		// TODO: should we wrap in a holder to indicate null entry?
		return bytes != null && bytes.length > 0 ? new String(bytes) : null;
	}

	@Override
	public void close() throws IOException {
		getStorage().close();
	}

}
