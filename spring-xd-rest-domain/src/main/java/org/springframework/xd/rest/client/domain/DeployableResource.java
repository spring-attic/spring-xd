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

package org.springframework.xd.rest.client.domain;

import javax.xml.bind.annotation.XmlAttribute;


/**
 * Abstract base class for resources that represent deployable things.
 *
 * @author Eric Bottard
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

//	@XmlAttribute
	private Boolean deployed;

	/**
	 * Return deployment status of the underlying thing, or {@code null} if unknown.
	 */
	public Boolean isDeployed() {
		return deployed;
	}

	public void setDeployed(Boolean deployed) {
		this.deployed = deployed;
	}

}
