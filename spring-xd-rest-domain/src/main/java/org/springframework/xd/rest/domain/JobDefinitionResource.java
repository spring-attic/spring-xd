/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.rest.domain;

import javax.xml.bind.annotation.XmlRootElement;

import org.springframework.hateoas.PagedResources;

/**
 * Represents a Job Definition.
 *
 * @author Glenn Renfro
 * @since 1.0
 */
@XmlRootElement
public class JobDefinitionResource extends DeployableResource {

	/**
	 * The DSL representation of this job definition.
	 */
	private String definition;

	/**
	 * Default constructor for serialization frameworks.
	 */
	@SuppressWarnings("unused")
	private JobDefinitionResource() {

	}

	public JobDefinitionResource(String name, String definition) {
		super(name);
		this.definition = definition;
	}

	public String getDefinition() {
		return definition;
	}

	/**
	 * Dedicated subclass to workaround type erasure.
	 *
	 * @author Eric Bottard
	 */
	public static class Page extends PagedResources<JobDefinitionResource> {

	}

}
