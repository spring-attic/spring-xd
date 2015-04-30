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

package org.springframework.xd.dirt.rest;

import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import org.springframework.xd.dirt.container.store.DetailedContainer;
import org.springframework.xd.dirt.module.store.ModuleMetadata;
import org.springframework.xd.rest.domain.ModuleMetadataResource;
import org.springframework.xd.rest.domain.DetailedContainerResource;

/**
 * Knows how to assemble {@link DetailedContainerResource}s out of {@link DetailedContainer} instances.
 *
 * @author Ilayaperumal Gopinathan
 * @author Mark Fisher
 */
public class RuntimeContainerResourceAssembler extends
		ResourceAssemblerSupport<DetailedContainer, DetailedContainerResource> {

	ResourceAssemblerSupport<ModuleMetadata, ModuleMetadataResource> moduleMetadatResourceAssembler = new ModuleMetadataResourceAssembler();

	public RuntimeContainerResourceAssembler() {
		super(ContainersController.class, DetailedContainerResource.class);
	}

	@Override
	public DetailedContainerResource toResource(DetailedContainer entity) {
		return createResourceWithId(entity.getName(), entity);
	}

	@Override
	protected DetailedContainerResource instantiateResource(DetailedContainer entity) {
		return new DetailedContainerResource(entity.getAttributes(), entity.getDeploymentSize(),
				moduleMetadatResourceAssembler.toResources(entity.getDeployedModules()), entity.getMessageRates());
	}

}
