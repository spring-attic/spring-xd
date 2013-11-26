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

package org.springframework.xd.dirt.stream;

import org.springframework.xd.dirt.core.BaseDefinition;

/**
 * Represents a model for a data flow in the system. This is typically described with a DSL expression of the form
 * {@code source | [processor | ]* sink}.
 * 
 * @see XDParser
 * 
 * @author Eric Bottard
 */
public class StreamDefinition extends BaseDefinition {

	@SuppressWarnings("unused")
	private StreamDefinition() {
		// no-arg constructor for serialization
	}

	/**
	 * Create a new StreamDefinition
	 * 
	 * @param name - the stream name
	 * @param definition - the stream definition
	 */
	public StreamDefinition(String name, String definition) {
		super(name, definition);
	}

}
