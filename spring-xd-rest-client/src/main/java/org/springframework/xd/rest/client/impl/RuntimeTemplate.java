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

package org.springframework.xd.rest.client.impl;

import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.xd.rest.client.RuntimeOperations;
import org.springframework.xd.rest.client.domain.ContainerMetadataResource;
import org.springframework.xd.rest.client.domain.ModuleMetadataResource;

/**
 * Implementation of the runtime containers/modules related part of the API.
 * 
 * @author Ilayaperumal Gopinathan
 */
public class RuntimeTemplate extends AbstractTemplate implements RuntimeOperations {

	RuntimeTemplate(AbstractTemplate source) {
		super(source);
	}

	@Override
	public ContainerMetadataResource.Page listRuntimeContainers() {
		String uriTemplate = resources.get("runtime/containers").toString();
		uriTemplate = uriTemplate + "?size=10000";
		return restTemplate.getForObject(uriTemplate, ContainerMetadataResource.Page.class);
	}

	@Override
	public ModuleMetadataResource.Page listRuntimeModules() {
		String uriTemplate = resources.get("runtime/modules").toString();
		uriTemplate = uriTemplate + "?size=10000";
		return restTemplate.getForObject(uriTemplate, ModuleMetadataResource.Page.class);
	}

	@Override
	public ModuleMetadataResource.Page listRuntimeModulesByContainer(String containerId) {
		String url = resources.get("runtime/modules").toString();
		String uriString = UriComponentsBuilder.fromUriString(url).queryParam("containerId", containerId).build().toUriString();
		return restTemplate.getForObject(uriString, ModuleMetadataResource.Page.class, containerId);
	}
}
