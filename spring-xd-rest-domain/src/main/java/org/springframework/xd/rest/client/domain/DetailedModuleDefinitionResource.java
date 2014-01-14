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

package org.springframework.xd.rest.client.domain;

import java.util.ArrayList;
import java.util.List;

import org.springframework.hateoas.PagedResources;


/**
 * Extends {@link ModuleDefinitionResource} with information about module options.
 * 
 * @author Eric Bottard
 */
public class DetailedModuleDefinitionResource extends ModuleDefinitionResource {

	/**
	 * Default constructor for serialization frameworks.
	 */
	protected DetailedModuleDefinitionResource() {

	}

	public DetailedModuleDefinitionResource(String name, String type, boolean composed) {
		super(name, type, composed);
	}

	private List<Option> options;

	public void addOption(Option option) {
		if (options == null) {
			options = new ArrayList<Option>();
		}
		options.add(option);
	}

	public static class Option {

		private String name;

		private String type;

		private String description;

		private String defaultValue;

		/**
		 * Default constructor for serialization frameworks.
		 */
		@SuppressWarnings("unused")
		private Option() {

		}

		public Option(String name, String type, String description, String defaultValue) {
			this.name = name;
			this.type = type;
			this.description = description;
			this.defaultValue = defaultValue;
		}


		public String getName() {
			return name;
		}


		public String getType() {
			return type;
		}


		public String getDescription() {
			return description;
		}


		public String getDefaultValue() {
			return defaultValue;
		}


	}


	public List<Option> getOptions() {
		return options;
	}

	/**
	 * Dedicated subclass to workaround type erasure.
	 * 
	 * @author Eric Bottard
	 */
	public static class Page extends PagedResources<DetailedModuleDefinitionResource> {

	}

}
