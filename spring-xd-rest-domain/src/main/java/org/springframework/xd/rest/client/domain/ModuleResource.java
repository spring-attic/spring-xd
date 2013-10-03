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

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlRootElement;

import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.ResourceSupport;


/**
 * Represents a runtime module
 * 
 * @author Ilayaperumal Gopinathan
 */
@XmlRootElement
public class ModuleResource extends ResourceSupport {

	private String moduleId;

	private String containerId;

	private String group;

	private String index;

	private String properties;

	@SuppressWarnings("unused")
	private ModuleResource() {
	}

	public ModuleResource(String containerId, String group, String index, String properties) {
		this.moduleId = containerId + ":" + group + ":" + index;
		this.containerId = containerId;
		this.group = group;
		this.index = index;
		this.properties = properties;
	}

	public String getModuleId() {
		return moduleId;
	}

	public String getContainerId() {
		return containerId;
	}

	public String getGroup() {
		return group;
	}

	public String getIndex() {
		return index;
	}

	public String getProperties() {
		return properties;
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
	public static class Page extends PagedResources<ModuleResource> {

	}

	public static class List extends ArrayList<ModuleResource> {

	}

}
