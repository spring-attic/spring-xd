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

package org.springframework.xd.hadoop.fs;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.util.Assert;

/**
 * 
 * @author Mark Pollack
 */
public class HdfsTextFileWriter extends AbstractHdfsWriter implements HdfsWriter {

	private FileSystem fileSystem;

	private FSDataOutputStream fsDataOutputStream;

	private volatile String charset = "UTF-8";

	public HdfsTextFileWriter(FileSystem fileSystem) {
		Assert.notNull(fileSystem, "Hadoop FileSystem must not be null.");
		this.fileSystem = fileSystem;
	}


	@Override
	public void write(Message<?> message) throws IOException {
		initializeCounterIfNecessary();
		prepareOutputStream();
		copy(getPayloadAsBytes(message), this.fsDataOutputStream);
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
				// this.fsDataOutputStream = this.getFileSystem().append(name);
				this.fsDataOutputStream = this.getFileSystem().create(name);
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

	@Override
	public FileSystem getFileSystem() {
		return this.fileSystem;
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

	// TODO note, taken from TcpMessageMapper
	/**
	 * Extracts the payload as a byte array.
	 * 
	 * @param message
	 * @return payload
	 */
	private byte[] getPayloadAsBytes(Message<?> message) {
		byte[] bytes = null;
		Object payload = message.getPayload();
		if (payload instanceof byte[]) {
			bytes = (byte[]) payload;
		}
		else if (payload instanceof String) {
			try {
				bytes = ((String) payload).getBytes(this.charset);
			}
			catch (UnsupportedEncodingException e) {
				throw new MessageHandlingException(message, e);
			}
		}
		else {
			throw new MessageHandlingException(message,
					"HdfsTextFileWriter expects " +
							"either a byte array or String payload, but received: " + payload.getClass());
		}
		return bytes;
	}

	@Override
	public void close() {
		if (fsDataOutputStream != null) {
			IOUtils.closeStream(fsDataOutputStream);
		}
	}

}
