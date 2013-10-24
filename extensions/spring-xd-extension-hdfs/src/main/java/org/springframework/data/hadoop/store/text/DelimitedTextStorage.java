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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Hashtable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.Seekable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.compress.SplitCompressionInputStream;
import org.apache.hadoop.util.LineReader;

import org.springframework.data.hadoop.store.AbstractStorage;
import org.springframework.data.hadoop.store.StorageReader;
import org.springframework.data.hadoop.store.StorageWriter;
import org.springframework.data.hadoop.store.codec.CodecInfo;
import org.springframework.data.hadoop.store.input.InputSplit;
import org.springframework.data.hadoop.store.support.DataUtils;
import org.springframework.data.hadoop.store.support.StreamsHolder;

/**
 * A {@code Storage} implementation using a delimited text as a record entry.
 * 
 * @author Janne Valkealahti
 * 
 */
public class DelimitedTextStorage extends AbstractStorage {

	private final static Log log = LogFactory.getLog(DelimitedTextStorage.class);

	/** Data delimiter */
	private final byte[] delimiter;

	/** Hadoop's line reader */
	private LineReader lineReader;

	/** Line readers for splits. */
	private Hashtable<InputSplit, LineReader> splitLineReaders = new Hashtable<InputSplit, LineReader>();

	/** Storage readers for splits. */
	private Hashtable<InputSplit, StorageReader> splitStorageReaders = new Hashtable<InputSplit, StorageReader>();

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
				OutputStream out = getOutput().getStream();
				out.write(bytes);
				out.write(delimiter);
				out.flush();
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
	public synchronized StorageReader getStorageReader(final InputSplit inputSplit) throws IOException {
		StorageReader splitStorageReader = splitStorageReaders.get(inputSplit);

		if (splitStorageReader == null) {

			final StreamsHolder<InputStream> holder = getInput(inputSplit);
			final LineReader splitReader = new LineReader(holder.getStream(), getConfiguration());
			splitLineReaders.put(inputSplit, splitReader);

			final long startx;
			final long endx;
			if (holder.getStream() instanceof SplitCompressionInputStream) {
				startx = ((SplitCompressionInputStream) holder.getStream()).getAdjustedStart();
				endx = ((SplitCompressionInputStream) holder.getStream()).getAdjustedEnd();
			}
			else {
				startx = inputSplit.getStart();
				endx = startx + inputSplit.getLength();
			}

			if (log.isDebugEnabled()) {
				log.debug("Split start=" + startx + " end=" + endx);
			}

			splitStorageReader = new StorageReader() {

				Seekable seekable = (Seekable) holder.getStream();

				long start = startx;

				long end = endx;

				long pos = start;

				@Override
				public byte[] read() throws IOException {
					long position = getFilePosition();
					if (position <= end) {
						Text text = new Text();
						int newSize = splitReader.readLine(text);
						pos += newSize;
						return text.getBytes();
					}
					else {
						return null;
					}
				}

				private long getFilePosition() throws IOException {
					long retVal;
					if (getCodec() != null && seekable != null) {
						retVal = seekable.getPos();
					}
					else {
						retVal = pos;
					}
					return retVal;
				}

			};
			splitStorageReaders.put(inputSplit, splitStorageReader);
		}

		return splitStorageReader;
	}

	@Override
	public synchronized void close() throws IOException {
		for (LineReader r : splitLineReaders.values()) {
			r.close();
		}
		splitLineReaders.clear();
		if (lineReader != null) {
			lineReader.close();
			lineReader = null;
		}
		super.close();
	}

}
