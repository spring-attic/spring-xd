/*
 * Copyright 2013-2014 the original author or authors.
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

import java.util.Properties;

import javax.xml.bind.annotation.XmlRootElement;

import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.ResourceSupport;


/**
 * Represents a runtime module.
 * 
 * @author Ilayaperumal Gopinathan
 */
@XmlRootElement
public class ModuleMetadataResource extends ResourceSupport {

	private String moduleId;

	private String name;

	private String unitName;

	private RESTModuleType moduleType;

	private String containerId;

	private Properties moduleOptions;

	private Properties deploymentProperties;

	private RESTDeploymentState deploymentStatus;

	@SuppressWarnings("unused")
	private ModuleMetadataResource() {
	}

	public ModuleMetadataResource(String moduleId, String name, String unitName, RESTModuleType moduleType,
			String containerId, Properties moduleProperties, Properties deploymentProperties,
			RESTDeploymentState deploymentStatus) {
		this.moduleId = moduleId;
		this.name = name;
		this.unitName = unitName;
		this.moduleType = moduleType;
		this.containerId = containerId;
		this.moduleOptions = moduleProperties;
		this.deploymentProperties = deploymentProperties;
		this.deploymentStatus = deploymentStatus;
	}

	public String getModuleId() {
		return moduleId;
	}

	public String getName() {
		return name;
	}

	public String getUnitName() {
		return this.unitName;
	}

	public RESTModuleType getModuleType() {
		return this.moduleType;
	}

	public String getContainerId() {
		return containerId;
	}

	public Properties getModuleOptions() {
		return moduleOptions;
	}

	public Properties getDeploymentProperties() {
		return deploymentProperties;
	}

	public RESTDeploymentState getDeploymentStatus() {
		return deploymentStatus;
	}

	@Override
	public String toString() {
		return this.moduleId;
	}

	/**
	 * Dedicated subclass to workaround type erasure.
	 * 
	 * @author Eric Bottard
	 */
	public static class Page extends PagedResources<ModuleMetadataResource> {

	}
}
