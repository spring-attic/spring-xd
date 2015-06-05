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

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.WriteFailedException;
import org.springframework.batch.item.file.transform.LineAggregator;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.hadoop.store.StoreException;
import org.springframework.util.Assert;

/**
 *
 * @author Mark Pollack
 */
public class HdfsTextItemWriter<T> extends AbstractHdfsItemWriter<T> implements InitializingBean {

	private static final String DEFAULT_LINE_SEPARATOR = System.getProperty("line.separator");

	private FileSystem fileSystem;

	private FSDataOutputStream fsDataOutputStream;

	private LineAggregator<T> lineAggregator;

	private String lineSeparator = DEFAULT_LINE_SEPARATOR;

	private volatile String charset = "UTF-8";

	public HdfsTextItemWriter(FileSystem fileSystem) {
		Assert.notNull(fileSystem, "Hadoop FileSystem must not be null.");
		this.fileSystem = fileSystem;
	}

	@Override
	public void write(List<? extends T> items) throws Exception {
		initializeCounterIfNecessary();
		prepareOutputStream();
		copy(getItemsAsBytes(items), this.fsDataOutputStream);
	}


	private void prepareOutputStream() throws IOException {
		boolean found = false;
		Path name = null;

		// TODO improve algorithm
		while (!found) {
			name = new Path(getFileName());
			// If it doesn't exist, create it. If it exists, return false
			if (getFileSystem().createNewFile(name)) {
				logger.debug("Created new HDFS file " + name.getName());
				found = true;
				this.resetBytesWritten();
				this.fsDataOutputStream = this.getFileSystem().append(name);
			}
			else {
				if (this.getBytesWritten() >= getRolloverThresholdInBytes()) {
					logger.debug("Rolling over new file");
					closeStream();
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
	private void copy(byte[] in, FSDataOutputStream out) throws IOException {
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
	 * @param items
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

	@Override
	public void update(ExecutionContext executionContext) {
		super.update(executionContext);
		logger.debug("Flushing output stream");
		if (fsDataOutputStream != null) {
			try {
				fsDataOutputStream.hflush();
			} catch (IOException e) {
				throw new StoreException("Error while flushing stream", e);
			}
		}
	}

	@Override
	public void close() {
		logger.debug("Closing item writer");
		closeStream();
		reset();
	}

	private void closeStream() {
		logger.debug("Closing output stream");
		if (fsDataOutputStream != null) {
			try {
				fsDataOutputStream.close();
			} catch (IOException e) {
				IOUtils.closeStream(fsDataOutputStream);
				throw new StoreException("Error while closing stream", e);
			} finally {
				fsDataOutputStream = null;
			}
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
