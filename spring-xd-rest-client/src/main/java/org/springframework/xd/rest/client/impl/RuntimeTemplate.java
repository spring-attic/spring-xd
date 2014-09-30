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

import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.xd.rest.client.RuntimeOperations;
import org.springframework.xd.rest.domain.DetailedContainerResource;
import org.springframework.xd.rest.domain.ModuleMetadataResource;

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
	public DetailedContainerResource.Page listContainers() {
		String uriTemplate = resources.get("runtime/containers").toString();
		uriTemplate = uriTemplate + "?size=10000";
		return restTemplate.getForObject(uriTemplate, DetailedContainerResource.Page.class);
	}

	@Override
	public ModuleMetadataResource.Page listDeployedModules() {
		String uriTemplate = resources.get("runtime/modules").toString();
		uriTemplate = uriTemplate + "?size=10000";
		return restTemplate.getForObject(uriTemplate, ModuleMetadataResource.Page.class);
	}

	@Override
	public ModuleMetadataResource listDeployedModule(String containerId, String moduleId) {
		Assert.isTrue(StringUtils.hasText(containerId));
		Assert.isTrue(StringUtils.hasText(moduleId));
		String url = resources.get("runtime/modules").toString();
		MultiValueMap<String, String> values = new LinkedMultiValueMap<String, String>();
		values.add("containerId", containerId);
		values.add("moduleId", moduleId);
		String uriString = UriComponentsBuilder.fromUriString(url).queryParams(values).build().toUriString();
		return restTemplate.getForObject(uriString, ModuleMetadataResource.class);
	}

	@Override
	public ModuleMetadataResource.Page listDeployedModulesByContainer(String containerId) {
		Assert.isTrue(StringUtils.hasText(containerId));
		String url = resources.get("runtime/modules").toString();
		String uriString = UriComponentsBuilder.fromUriString(url).queryParam("containerId", containerId).build().toUriString();
		return restTemplate.getForObject(uriString, ModuleMetadataResource.Page.class);
	}

	@Override
	public ModuleMetadataResource.Page listDeployedModulesByModuleId(String moduleId) {
		Assert.isTrue(StringUtils.hasText(moduleId));
		String url = resources.get("runtime/modules").toString();
		String uriString = UriComponentsBuilder.fromUriString(url).queryParam("moduleId", moduleId).build().toUriString();
		return restTemplate.getForObject(uriString, ModuleMetadataResource.Page.class);
	}
}
