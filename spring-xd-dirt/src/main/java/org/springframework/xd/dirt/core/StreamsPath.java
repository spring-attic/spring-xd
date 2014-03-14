/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.xd.dirt.core;

import org.springframework.util.Assert;
import org.springframework.xd.dirt.zookeeper.Paths;

/**
 * Builder object for paths under {@link Paths#STREAMS}. {@code StreamsPath} can be used to take a full path and split
 * it into its elements, for example:
 * <p>
 * <code>
 * StreamsPath streamsPath = new StreamsPath("/xd/streams/my-stream");
 * assertEquals("my-stream", streamsPath.getStreamName());
 * </code>
 * </p>
 * It can also be used to build a path, for example:
 * <p>
 * <code>
 * StreamsPath streamsPath = new StreamsPath().setStreamName("my-stream");
 * assertEquals("/streams/my-stream", streamsPath.build());
 * </code>
 * </p>
 * 
 * @author Patrick Peralta
 */
public class StreamsPath {

	/**
	 * Index for {@link Paths#STREAMS} in {@link #elements} array.
	 */
	private static final int STREAMS = 0;

	/**
	 * Index for stream name in {@link #elements} array.
	 */
	private static final int STREAM_NAME = 1;

	/**
	 * Index for module type in {@link #elements} array.
	 */
	private static final int MODULE_TYPE = 2;

	/**
	 * Index for module label in {@link #elements} array.
	 */
	private static final int MODULE_LABEL = 3;

	/**
	 * Index for container name in {@link #elements} array.
	 */
	private static final int CONTAINER = 4;

	/**
	 * Array of path elements.
	 */
	private final String[] elements = new String[5];


	/**
	 * Construct a {@code StreamsPath}. Use of this constructor means that a path will be created via {@link #build()}
	 * or {@link #buildWithNamespace()}.
	 */
	public StreamsPath() {
		elements[STREAMS] = Paths.STREAMS;
	}

	/**
	 * Construct a {@code StreamsPath}. Use of this constructor means that an existing path will be provided and this
	 * object will be used to extract the individual elements of the path. Both full paths (including and excluding the
	 * {@link Paths#XD_NAMESPACE XD namespace prefix}) are supported.
	 * 
	 * @param path stream path
	 */
	public StreamsPath(String path) {
		Assert.hasText(path);

		String[] pathElements = path.split("\\/");

		// offset is the element array that contains the 'streams'
		// path element; the location may vary depending on whether
		// the path string includes the '/xd' namespace
		int offset = -1;
		for (int i = 0; i < pathElements.length; i++) {
			if (pathElements[i].equals(Paths.STREAMS)) {
				offset = i;
				break;
			}
		}

		if (offset == -1) {
			throw new IllegalArgumentException(String.format(
					"Path '%s' does not include a '%s' element", path, Paths.STREAMS));
		}

		System.arraycopy(pathElements, offset, elements, 0, pathElements.length - offset);

		Assert.state(elements[STREAMS].equals(Paths.STREAMS));
	}

	/**
	 * Return the stream name.
	 * 
	 * @return stream name
	 */
	public String getStreamName() {
		return elements[STREAM_NAME];
	}

	/**
	 * Set the stream name.
	 * 
	 * @param name stream name
	 * 
	 * @return this object
	 */
	public StreamsPath setStreamName(String name) {
		elements[STREAM_NAME] = name;
		return this;
	}

	/**
	 * Return the module type.
	 * 
	 * @return module type
	 */
	public String getModuleType() {
		return elements[MODULE_TYPE];
	}

	/**
	 * Set the module type.
	 * 
	 * @param type module type
	 * 
	 * @return this object
	 */
	public StreamsPath setModuleType(String type) {
		elements[MODULE_TYPE] = type;
		return this;
	}

	/**
	 * Return the module label.
	 * 
	 * @return module label
	 */
	public String getModuleLabel() {
		return elements[MODULE_LABEL];
	}

	/**
	 * Set the module label.
	 * 
	 * @param label module label
	 * 
	 * @return this object
	 */
	public StreamsPath setModuleLabel(String label) {
		elements[MODULE_LABEL] = label;
		return this;
	}

	/**
	 * Return the container name.
	 * 
	 * @return container name
	 */
	public String getContainer() {
		return elements[CONTAINER];
	}

	/**
	 * Set the container name.
	 * 
	 * @param container container name
	 * 
	 * @return this object
	 */
	public StreamsPath setContainer(String container) {
		elements[CONTAINER] = container;
		return this;
	}

	/**
	 * Build the path string using the field values.
	 * 
	 * @return path string
	 * 
	 * @see Paths#build
	 */
	public String build() {
		return Paths.build(stripNullElements());
	}

	/**
	 * Build the path string using the field values, including the namespace prefix.
	 * 
	 * @return path string with namespace
	 * 
	 * @see Paths#buildWithNamespace
	 */
	public String buildWithNamespace() {
		return Paths.buildWithNamespace(stripNullElements());
	}

	/**
	 * Return an array omitting the null values in {@link #elements}.
	 * 
	 * @return {@code elements} array without null values
	 */
	protected String[] stripNullElements() {
		int i = elements.length;

		// assuming that 'i' will never be < 0 because
		// both constructors assign a value to elements[0]
		while (elements[i - 1] == null) {
			--i;
		}
		if (i == elements.length) {
			return elements;
		}
		String[] s = new String[i];
		System.arraycopy(elements, 0, s, 0, i);
		return s;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return build();
	}

}
