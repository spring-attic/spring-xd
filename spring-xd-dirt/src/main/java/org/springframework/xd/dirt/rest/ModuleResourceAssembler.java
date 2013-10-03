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

package org.springframework.xd.dirt.rest;

import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import org.springframework.xd.dirt.module.store.ModuleEntity;
import org.springframework.xd.rest.client.domain.ModuleResource;


/**
 * Knows how to assemble {@link ModuleResource}s out of {@link ModuleEntity}s.
 * 
 * @author Ilayaperumal Gopinathan
 */
public class ModuleResourceAssembler extends ResourceAssemblerSupport<ModuleEntity, ModuleResource> {

	public ModuleResourceAssembler() {
		super(RuntimeModulesController.class, ModuleResource.class);
	}

	@Override
	public ModuleResource toResource(ModuleEntity entity) {
		return createResourceWithId(entity.getId(), entity);
	}

	@Override
	protected ModuleResource instantiateResource(ModuleEntity entity) {
		return new ModuleResource(entity.getContainerId(), entity.getGroup(), entity.getIndex(), entity.getProperties());
	}

}
