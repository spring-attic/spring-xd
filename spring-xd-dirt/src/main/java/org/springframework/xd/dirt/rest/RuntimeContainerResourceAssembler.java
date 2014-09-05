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
import org.springframework.xd.dirt.cluster.RuntimeContainer;
import org.springframework.xd.dirt.module.store.ModuleMetadata;
import org.springframework.xd.rest.domain.ModuleMetadataResource;
import org.springframework.xd.rest.domain.RuntimeContainerResource;

/**
 * Knows how to assemble {@link RuntimeContainerResource}s out of {@link RuntimeContainer} instances.
 *
 * @author Ilayaperumal Gopinathan
 * @author Mark Fisher
 */
public class RuntimeContainerResourceAssembler extends
		ResourceAssemblerSupport<RuntimeContainer, RuntimeContainerResource> {

	ResourceAssemblerSupport<ModuleMetadata, ModuleMetadataResource> moduleMetadatResourceAssembler = new ModuleMetadataResourceAssembler();

	public RuntimeContainerResourceAssembler() {
		super(RuntimeContainersController.class, RuntimeContainerResource.class);
	}

	@Override
	public RuntimeContainerResource toResource(RuntimeContainer entity) {
		return createResourceWithId(entity.getName(), entity);
	}

	@Override
	protected RuntimeContainerResource instantiateResource(RuntimeContainer entity) {
		return new RuntimeContainerResource(entity.getAttributes(), entity.getDeploymentSize(),
				moduleMetadatResourceAssembler.toResources(entity.getDeployedModules()), entity.getMessageRates());
	}

}
