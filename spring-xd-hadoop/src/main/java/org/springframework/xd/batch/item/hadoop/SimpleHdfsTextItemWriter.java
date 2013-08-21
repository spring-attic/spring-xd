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

package org.springframework.xd.batch.item.hadoop;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;

import org.springframework.batch.item.WriteFailedException;
import org.springframework.batch.item.file.transform.LineAggregator;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * 
 * @author Mark Pollack
 */
public class SimpleHdfsTextItemWriter<T> extends SimpleAbstractHdfsItemWriter<T> implements InitializingBean {

	private static final String DEFAULT_LINE_SEPARATOR = System.getProperty("line.separator");

	private FileSystem fileSystem;

	private FSDataOutputStream fsDataOutputStream;

	private LineAggregator<T> lineAggregator;

	private String lineSeparator = DEFAULT_LINE_SEPARATOR;

	private volatile String charset = "UTF-8";

	public SimpleHdfsTextItemWriter(FileSystem fileSystem) {
		Assert.notNull(fileSystem, "Hadoop FileSystem must not be null.");
		this.fileSystem = fileSystem;
	}

	@Override
	public void write(List<? extends T> items) throws Exception {
		// open (prepare)
		initializeCounterIfNecessary();
		prepareOutputStream();

		// write
		copy(getItemsAsBytes(items), this.fsDataOutputStream);

		// close
	}


	private void prepareOutputStream() throws IOException {
		boolean found = false;
		Path name = null;

		// TODO improve algorithm
		while (!found) {
			name = new Path(getFileName());
			// If it doesn't exist, create it. If it exists, return false
			if (getFileSystem().createNewFile(name)) {
				found = true;
				this.resetBytesWritten();
				this.fsDataOutputStream = this.getFileSystem().append(name);
			}
			else {
				if (this.getBytesWritten() >= getRolloverThresholdInBytes()) {
					close();
					incrementCounter();
				}
				else {
					found = true;
				}
			}
		}
	}


	/**
	 * Simple not optimized copy
	 */
	public void copy(byte[] in, FSDataOutputStream out) throws IOException {
		Assert.notNull(in, "No input byte array specified");
		Assert.notNull(out, "No OutputStream specified");
		out.write(in);
		incrementBytesWritten(in.length);
	}

	@Override
	public FileSystem getFileSystem() {
		return this.fileSystem;
	}

	/**
	 * Converts the list of items to a byte array.
	 * 
	 * @param items the list
	 * @return the byte array
	 */
	private byte[] getItemsAsBytes(List<? extends T> items) {

		StringBuilder lines = new StringBuilder();
		for (T item : items) {
			lines.append(lineAggregator.aggregate(item) + lineSeparator);
		}
		try {
			return lines.toString().getBytes(this.charset);
		}
		catch (UnsupportedEncodingException e) {
			throw new WriteFailedException("Could not write data.", e);
		}
	}


	public void close() {
		if (fsDataOutputStream != null) {
			IOUtils.closeStream(fsDataOutputStream);
		}
	}

	/**
	 * Public setter for the {@link LineAggregator}. This will be used to translate the item into a line for output.
	 * 
	 * @param lineAggregator the {@link LineAggregator} to set
	 */
	public void setLineAggregator(LineAggregator<T> lineAggregator) {
		this.lineAggregator = lineAggregator;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(lineAggregator, "A LineAggregator must be provided.");
	}


}
