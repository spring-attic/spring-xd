/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.xd.rest.domain;

import org.springframework.hateoas.ResourceSupport;

/**
 * Abstract base class for all resources that have a name.
 * 
 * @author Eric Bottard
 */
public abstract class NamedResource extends ResourceSupport {

	private String name;

	/**
	 * No arg constructor for serialization frameworks.
	 */
	protected NamedResource() {

	}

	public NamedResource(String name) {
		this.name = name;
	}

	/**
	 * Return the name of the represented entity.
	 */
	public String getName() {
		return name;
	}

}
