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

package org.springframework.data.hadoop.store2.input;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.SequenceFile.Reader;
import org.apache.hadoop.io.Text;

import org.springframework.data.hadoop.store2.DataStoreReader;
import org.springframework.data.hadoop.store2.codec.CodecInfo;

public class TextSequenceFileReader extends AbstractSequenceFileReader implements DataStoreReader<String> {

	private Reader reader;

	public TextSequenceFileReader(Configuration configuration, Path basePath, CodecInfo codec) {
		super(configuration, basePath, codec);
	}

	@Override
	public void close() throws IOException {
		reader.close();
	}

	@Override
	public String read() throws IOException {
		if (reader == null) {
			reader = getInput();
		}

		Text k = new Text();
		Text v = new Text();
		reader.next(k, v);
		byte[] value = v.getBytes();
		return value != null && value.length > 0 ? new String(value) : null;
	}


}
