/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.xd.rest.client.impl;

import org.springframework.xd.rest.client.ModuleOperations;
import org.springframework.xd.rest.client.domain.ModuleDefinitionResource;

/**
 * Implementation of the Module-related part of the API.
 * 
 * @author Glenn Renfro
 */
public class ModuleTemplate extends AbstractTemplate implements ModuleOperations {

	ModuleTemplate(AbstractTemplate source) {
		super(source);
	}

	@Override
	public ModuleDefinitionResource composeModule(String name, String definition) {
		return null;
	}


	@Override
	public ModuleDefinitionResource.Page list(String type) {
		String uriTemplate = resources.get("modules").toString();
		// TODO handle pagination at the client side
		uriTemplate = uriTemplate + "?size=10000&type=" + type;
		return restTemplate.getForObject(uriTemplate, ModuleDefinitionResource.Page.class);
	}


	@Override
	public String toString() {
		return "ModuleTemplate [restTemplate=" + restTemplate + ", resources=" + resources + "]";
	}

}
