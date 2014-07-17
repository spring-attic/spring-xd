/*
 * Copyright 2002-2014 the original author or authors.
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

/**
 * Abstract base class for resources that represent deployable units.
 * 
 * @author Eric Bottard
 * @author Mark Fisher
 */
public abstract class DeployableResource extends NamedResource {

	/**
	 * No arg constructor for serialization frameworks.
	 */
	protected DeployableResource() {
	}

	public DeployableResource(String name) {
		super(name);
	}

	// @XmlAttribute
	private String status;

	/**
	 * Return status of the underlying deployment unit, or {@code null} if unknown.
	 */
	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

}
