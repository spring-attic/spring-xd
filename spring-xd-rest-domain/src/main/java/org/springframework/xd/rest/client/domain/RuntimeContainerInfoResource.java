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

import javax.xml.bind.annotation.XmlRootElement;

import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.util.Assert;


/**
 * Represents runtime container info model.
 * 
 * @author Ilayaperumal Gopinathan
 */
@XmlRootElement
public class RuntimeContainerInfoResource extends ResourceSupport {

	private String containerId;

	private String jvmName;

	private String hostName;

	private String ipAddress;

	@SuppressWarnings("unused")
	private RuntimeContainerInfoResource() {
	}

	public RuntimeContainerInfoResource(String containerId, String jvmName, String hostName, String ipAddress) {
		Assert.hasText(containerId, "Container Id can not be empty");
		Assert.hasText(jvmName, "JVM name can not be empty");
		Assert.hasText(hostName, "Hostname can not be empty");
		Assert.hasText(ipAddress, "IP address can not be empty");
		this.containerId = containerId;
		this.jvmName = jvmName;
		this.hostName = hostName;
		this.ipAddress = ipAddress;
	}

	public String getContainerId() {
		return containerId;
	}

	public String getJvmName() {
		return jvmName;
	}

	public String getHostName() {
		return hostName;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	@Override
	public String toString() {
		return this.containerId;
	}

	/**
	 * Dedicated subclass to workaround type erasure.
	 * 
	 * @author Eric Bottard
	 */
	public static class Page extends PagedResources<RuntimeContainerInfoResource> {

	}

}
