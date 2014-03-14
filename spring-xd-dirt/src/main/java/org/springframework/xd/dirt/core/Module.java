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

/**
 * Domain object for an XD module.
 * 
 * @author Patrick Peralta
 */
public class Module {

	/**
	 * Type of module.
	 */
	public enum Type {
		/**
		 * Source module type. Indicates that this module will be a data source (and thus the first module indicated in
		 * a stream definition).
		 */
		SOURCE,

		/**
		 * Sink module type. Indicates that this module will be a data sink (and thus the last module indicated in a
		 * stream definition).
		 */
		SINK,

		/**
		 * Processor module type. Indicates that this module processes data as it moves from a {@link #SOURCE} to a
		 * {@link #SINK} module.
		 */
		PROCESSOR;

		/**
		 * {@inheritDoc}
		 * <p/>
		 * Returns the type in lower case. This is typically used for module type naming in a repository.
		 */
		@Override
		public String toString() {
			return name().toLowerCase();
		}
	}

	/**
	 * Module type.
	 */
	private final Type type;

	/**
	 * Module name.
	 */
	private final String name;

	/**
	 * URL for loading this module.
	 */
	private final String url;

	/**
	 * Construct a Module.
	 * 
	 * @param name name of module
	 * @param type module type
	 * @param url URL where this module can be loaded
	 */
	public Module(String name, Type type, String url) {
		this.name = name;
		this.type = type;
		this.url = url;
	}

	/**
	 * Return the module type.
	 * 
	 * @return module type
	 */
	public Type getType() {
		return type;
	}

	/**
	 * Return the module name.
	 * 
	 * @return module name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Return the URL where this module can be loaded.
	 * 
	 * @return URL where this module can be loaded
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return "Module{" +
				"type=" + type +
				", name='" + name + '\'' +
				", url='" + url + '\'' +
				'}';
	}

}
