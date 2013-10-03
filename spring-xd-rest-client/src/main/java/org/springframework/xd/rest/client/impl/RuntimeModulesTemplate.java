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

package org.springframework.xd.rest.client.impl;

import org.springframework.xd.rest.client.RuntimeModulesOperations;
import org.springframework.xd.rest.client.domain.ModuleResource;


/**
 * Implementation of the runtime modules related part of the API.
 * 
 * @author Ilayaperumal Gopinathan
 */
public class RuntimeModulesTemplate extends AbstractTemplate implements RuntimeModulesOperations {

	RuntimeModulesTemplate(AbstractTemplate source) {
		super(source);
	}

	@Override
	public ModuleResource.Page list() {
		String uriTemplate = resources.get("runtime/modules").toString();
		uriTemplate = uriTemplate + "?size=10000";
		return restTemplate.getForObject(uriTemplate, ModuleResource.Page.class);
	}

	@Override
	public ModuleResource.List listByContainer(String containerId) {
		String url = resources.get("runtime/containers").toString() + "/{containerId}/modules";
		return restTemplate.getForObject(url, ModuleResource.List.class, containerId);
	}
}
