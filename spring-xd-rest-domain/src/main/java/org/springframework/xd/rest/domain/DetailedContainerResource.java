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

package org.springframework.xd.rest.domain;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.util.Assert;

/**
 * Represents container info model.
 *
 * @author Ilayaperumal Gopinathan
 */
@XmlRootElement
public class DetailedContainerResource extends ResourceSupport {

	private String containerId;

	private String groups;

	private int deploymentSize;

	private List<ModuleMetadataResource> deployedModules;

	private Map<String, HashMap<String, Double>> messageRates;

	private Map<String, String> attributes;

	@SuppressWarnings("unused")
	private DetailedContainerResource() {
	}

	/**
	 * Construct ContainerResource using the container attributes and
	 * deployed modules.
	 *
	 * @param attributes the container attributes
	 * @param deploymentSize number of deployed modules
	 * @param deployedModules the list of deployed modules
	 * @param messageRates the messageRates for deployed modules
	 */
	public DetailedContainerResource(Map<String, String> attributes, int deploymentSize,
			List<ModuleMetadataResource> deployedModules,
			Map<String, HashMap<String, Double>> messageRates) {
		Assert.notNull(attributes);
		this.containerId = attributes.get("id");
		this.groups = attributes.get("groups");
		this.attributes = attributes;
		this.deploymentSize = deploymentSize;
		this.deployedModules = deployedModules;
		this.messageRates = messageRates;
	}

	/**
	 * Get all the container attributes.
	 *
	 * @return all the container attributes.
	 */
	public Map<String, String> getAttributes() {
		return this.attributes;
	}

	/**
	 * Get the attribute value by given name.
	 *
	 * @param name the attribute name
	 * @return value corresponding to the given attribute name.
	 */
	public String getAttribute(String name) {
		return this.attributes.get(name);
	}

	/**
	 * Get container id.
	 *
	 * @return the container id.
	 */
	public String getContainerId() {
		return this.containerId;
	}

	/**
	 * Get container group(s).
	 *
	 * @return the container groups that the container belongs to.
	 */
	public String getGroups() {
		return this.groups;
	}

	/**
	 * Get the number of modules deployed into the container.
	 *
	 * @return the count of deployed modules.
	 */
	public int getDeploymentSize() {
		return this.deploymentSize;
	}

	/**
	 * Get the list of deployed modules.
	 *
	 * @return the deployed modules.
	 */
	public List<ModuleMetadataResource> getDeployedModules() {
		return this.deployedModules;
	}

	public Map<String, HashMap<String, Double>> getMessageRates() {
		return this.messageRates;
	}

	@Override
	public String toString() {
		return attributes.toString();
	}


	/**
	 * Dedicated subclass to workaround type erasure.
	 *
	 * @author Eric Bottard
	 */
	public static class Page extends PagedResources<DetailedContainerResource> {

	}

}
