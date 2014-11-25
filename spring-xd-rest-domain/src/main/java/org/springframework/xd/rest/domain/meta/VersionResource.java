/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.xd.rest.domain.meta;

import javax.xml.bind.annotation.XmlRootElement;

import org.springframework.hateoas.ResourceSupport;
import org.springframework.util.Assert;

/**
 * Provides the version information for Spring XD.
 *
 * @author Gunnar Hillert
 */
@XmlRootElement
public class VersionResource extends ResourceSupport {

	private boolean versionAvailable;

	private String version;

	/**
	 * Default constructor for serialization frameworks.
	 */
	public VersionResource() {
	}

	/**
	 * Default constructor for serialization frameworks.
	 */
	public VersionResource(String version) {
		Assert.hasText(version, "The version must not be empty.");
		this.version = version;
		this.versionAvailable = true;
	}

	public boolean isVersionAvailable() {
		return versionAvailable;
	}

	public String getVersion() {
		return version;
	}

}
