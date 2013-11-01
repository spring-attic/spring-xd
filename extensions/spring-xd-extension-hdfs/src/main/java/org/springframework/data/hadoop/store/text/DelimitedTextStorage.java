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
import java.io.OutputStream;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.util.LineReader;

import org.springframework.data.hadoop.store.AbstractStrategiesStorage;
import org.springframework.data.hadoop.store.StorageReader;
import org.springframework.data.hadoop.store.StorageWriter;
import org.springframework.data.hadoop.store.codec.CodecInfo;
import org.springframework.data.hadoop.store.support.DataUtils;
import org.springframework.data.hadoop.store.support.StreamsHolder;

/**
 * A {@code Storage} implementation using a delimited text as a record entry.
 * 
 * @author Janne Valkealahti
 * 
 */
public class DelimitedTextStorage extends AbstractStrategiesStorage {

	/** Data delimiter */
	private final byte[] delimiter;

	/** Hadoop's line reader */
	private LineReader lineReader;

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
	public synchronized StorageWriter getStorageWriter() throws IOException {
		return new StorageWriter() {

			@Override
			public void write(byte[] bytes) throws IOException {
				StreamsHolder<OutputStream> holder = getOutput();
				OutputStream out = holder.getStream();
				OutputStream wout = holder.getWrappedStream();
				out.write(bytes);
				out.write(delimiter);
				out.flush();

				// TODO: improve should not do casting
				if (out instanceof FSDataOutputStream) {
					reportSizeAware(((FSDataOutputStream) out).getPos());
				}
				else if (wout instanceof FSDataOutputStream) {
					reportSizeAware(((FSDataOutputStream) wout).getPos());
				}
			}
		};
	}

	@Override
	public synchronized StorageReader getStorageReader(Path path) throws IOException {
		if (lineReader == null) {
			lineReader = new LineReader(getInput(path).getStream(), getConfiguration());
		}
		return new StorageReader() {

			@Override
			public byte[] read() throws IOException {
				Text text = new Text();
				lineReader.readLine(text);
				return text.getBytes();
			}
		};
	}

	@Override
	public synchronized void close() throws IOException {
		if (lineReader != null) {
			lineReader.close();
			lineReader = null;
		}
		super.close();
	}

}
