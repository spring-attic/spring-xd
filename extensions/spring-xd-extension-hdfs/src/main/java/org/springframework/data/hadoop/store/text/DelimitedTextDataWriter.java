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
import java.io.OutputStream;

import org.apache.hadoop.fs.FSDataOutputStream;

import org.springframework.data.hadoop.store.DataWriter;
import org.springframework.data.hadoop.store.event.FileWrittenEvent;
import org.springframework.data.hadoop.store.support.LifecycleObjectSupport;
import org.springframework.data.hadoop.store.support.StreamsHolder;

/**
 * A {@code DataWriter} implementation for writing delimited text.
 * 
 * @author Janne Valkealahti
 * 
 */
public class DelimitedTextDataWriter extends LifecycleObjectSupport implements DataWriter {

	private StreamsHolder<OutputStream> holder;

	private final byte[] delimiter;

	/**
	 * Instantiates a new delimited text data writer.
	 * 
	 * @param holder the holder
	 * @param delimiter the delimiter
	 */
	public DelimitedTextDataWriter(StreamsHolder<OutputStream> holder, byte[] delimiter) {
		this.holder = holder;
		this.delimiter = delimiter;
	}

	@Override
	public void write(byte[] value) throws IOException {
		OutputStream out = holder.getStream();
		out.write(value);
		out.write(delimiter);
	}

	@Override
	public void flush() throws IOException {
		holder.getStream().flush();
	}

	@Override
	public void close() throws IOException {
		holder.close();
		if (getStorageEventPublisher() != null) {
			getStorageEventPublisher().publishEvent(new FileWrittenEvent(this, holder.getPath()));
		}
	}

	@Override
	public long getPosition() throws IOException {
		OutputStream out = holder.getStream();
		OutputStream wout = holder.getWrappedStream();
		if (out instanceof FSDataOutputStream) {
			return ((FSDataOutputStream) out).getPos();
		}
		else if (wout instanceof FSDataOutputStream) {
			return ((FSDataOutputStream) wout).getPos();
		}
		else {
			return 0;
		}
	}

}
