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

package org.springframework.data.hadoop.store.support;

/**
 * Holder object streams. Mostly concept of a wrapped streams are used with a compressed streams in a hadoop where some
 * information still needs to be accessed from an underlying stream.
 * 
 * @param <T> a type of a stream
 * @author Janne Valkealahti
 * 
 */
public class StreamsHolder<T> {

	private T stream;

	private T wrappedStream;

	/**
	 * Instantiates a new streams holder.
	 */
	public StreamsHolder() {
	}

	/**
	 * Instantiates a new streams holder.
	 * 
	 * @param stream the stream
	 * @param wrappedStream the wrapped stream
	 */
	public StreamsHolder(T stream, T wrappedStream) {
		super();
		this.stream = stream;
		this.wrappedStream = wrappedStream;
	}

	/**
	 * Gets the stream.
	 * 
	 * @return the stream
	 */
	public T getStream() {
		return stream;
	}

	/**
	 * Sets the stream.
	 * 
	 * @param stream the new stream
	 */
	public void setStream(T stream) {
		this.stream = stream;
	}

	/**
	 * Gets the wrapped stream.
	 * 
	 * @return the wrapped stream
	 */
	public T getWrappedStream() {
		return wrappedStream;
	}

	/**
	 * Sets the wrapped stream.
	 * 
	 * @param wrappedStream the new wrapped stream
	 */
	public void setWrappedStream(T wrappedStream) {
		this.wrappedStream = wrappedStream;
	}

}
