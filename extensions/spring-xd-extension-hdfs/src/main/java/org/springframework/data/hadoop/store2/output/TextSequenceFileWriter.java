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

package org.springframework.data.hadoop.store2.output;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.SequenceFile.Writer;
import org.apache.hadoop.io.Text;

import org.springframework.data.hadoop.store2.DataStoreWriter;
import org.springframework.data.hadoop.store2.codec.CodecInfo;

public class TextSequenceFileWriter extends AbstractSequenceFileWriter implements DataStoreWriter<String> {

	private Writer writer;

	private final static Text NULL_KEY = new Text(new byte[0]);

	public TextSequenceFileWriter(Configuration configuration, Path basePath, CodecInfo codec) {
		super(configuration, basePath, codec);
	}

	@Override
	public void flush() throws IOException {
	}

	@Override
	public void close() throws IOException {
		writer.close();
	}

	@Override
	public void write(String entity) throws IOException {
		if (writer == null) {
			writer = getOutput();
		}
		writer.append(NULL_KEY, new Text(entity.getBytes()));
	}

}
