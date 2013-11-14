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

import javax.xml.bind.annotation.XmlRootElement;

import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.ResourceSupport;

/**
 * Represents a Module Definition.
 * 
 * @author Glenn Renfro
 * @author Mark Fisher
 * @since 1.0
 */
@XmlRootElement
public class ModuleDefinitionResource extends ResourceSupport {

	private volatile String name;

	private volatile String type;

	private volatile String moduleId;

	/**
	 * The DSL representation of this module definition.
	 */
	private volatile String definition;

	/**
	 * Default constructor for serialization frameworks.
	 */
	@SuppressWarnings("unused")
	private ModuleDefinitionResource() {
	}

	public ModuleDefinitionResource(String name, String definition, String type) {
		this.moduleId = type + ":" + name;
		this.name = name;
		this.definition = definition;
		this.type = type;
	}

	public String getModuleId() {
		return moduleId;
	}

	public String getName() {
		return name;
	}

	public String getType() {
		return type;
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
