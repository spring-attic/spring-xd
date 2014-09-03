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

import javax.xml.bind.annotation.XmlRootElement;

import org.springframework.hateoas.ResourceSupport;


/**
 * Represents {@ModuleDescriptor}.
 *
 * @author Ilayaperumal Gopinathan
 */
@XmlRootElement
public class ModuleDescriptorResource extends ResourceSupport {

	private String moduleLabel;

	private String moduleType;

	@SuppressWarnings("unused")
	private ModuleDescriptorResource() {
	}

	public ModuleDescriptorResource(String moduleLabel, String moduleType) {
		this.moduleLabel = moduleLabel;
		this.moduleType = moduleType;
	}

	public String getModuleLabel() {
		return moduleLabel;
	}

	public void setModuleLabel(String moduleLabel) {
		this.moduleLabel = moduleLabel;
	}

	public String getModuleType() {
		return moduleType;
	}

	public void setModuleType(String moduleType) {
		this.moduleType = moduleType;
	}

}
