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

package org.springframework.data.hadoop.store.text;

import java.io.IOException;
import java.io.InputStream;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.util.LineReader;

import org.springframework.data.hadoop.store.DataReader;
import org.springframework.data.hadoop.store.support.StreamsHolder;

/**
 * A {@code DataReader} implementation for reading delimited text.
 * 
 * @author Janne Valkealahti
 * 
 */
public class DelimitedTextDataReader implements DataReader {

	private StreamsHolder<InputStream> holder;

	private LineReader lineReader;

	/**
	 * Instantiates a new delimited text data reader.
	 * 
	 * @param holder the holder
	 */
	public DelimitedTextDataReader(StreamsHolder<InputStream> holder) {
		this.holder = holder;
		lineReader = new LineReader(holder.getStream());
	}

	@Override
	public void close() throws IOException {
		if (lineReader != null) {
			lineReader.close();
			lineReader = null;
		}
		holder.close();
	}

	@Override
	public byte[] read() throws IOException {
		Text text = new Text();
		lineReader.readLine(text);
		return text.getBytes();
	}

}
