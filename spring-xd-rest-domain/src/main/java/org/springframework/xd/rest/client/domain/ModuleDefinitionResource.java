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

package org.springframework.xd.rest.client.domain;

import org.springframework.hateoas.PagedResources;

/**
 * Represents a Module Definition.
 * 
 * @author Glenn Renfro
 * @since 1.0
 */
public class ModuleDefinitionResource extends NamedResource {

	private String type;

	public String getType() {
		return type;
	}

	/**
	 * The DSL representation of this module definition.
	 */
	private String definition;

	/**
	 * Default constructor for serialization frameworks.
	 */
	@SuppressWarnings("unused")
	private ModuleDefinitionResource() {

	}

	public ModuleDefinitionResource(String name, String definition, String type) {
		super(name);
		this.definition = definition;
		this.type = type;
	}

	public String getDefinition() {
		return definition;
	}

	/**
	 * Dedicated subclass to workaround type erasure.
	 * 
	 * @author Eric Bottard
	 */
	public static class Page extends PagedResources<ModuleDefinitionResource> {

	}

}
