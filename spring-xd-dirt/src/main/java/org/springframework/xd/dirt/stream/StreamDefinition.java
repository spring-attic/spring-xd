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

import org.springframework.util.Assert;

/**
 * Represents a model for a data flow in the system. This is typically described with a DSL expression of the form
 * {@code source | [processor | ]* sink}.
 * 
 * @see StreamParser
 * 
 * @author Eric Bottard
 */
public class StreamDefinition {

	/**
	 * A String representation of the desired data flow, expressed in the XD DSL.
	 */
	private String definition;

	/**
	 * A unique name given to that stream definition.
	 */
	private String name;

	public StreamDefinition(String name, String definition) {
		Assert.hasText(name, "Name can not be empty");
		Assert.hasText(definition, "Definition can not be empty");
		this.name = name;
		this.definition = definition;
	}

	/**
	 * Returns a String representation of the desired data flow, expressed in the XD DSL.
	 */
	public String getDefinition() {
		return definition;
	}

	/**
	 * Returns a unique name given to that stream definition.
	 */
	public String getName() {
		return name;
	}

}
