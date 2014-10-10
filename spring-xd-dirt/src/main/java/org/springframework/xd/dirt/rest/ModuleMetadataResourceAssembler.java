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
import org.springframework.xd.dirt.module.store.ModuleMetadata;
import org.springframework.xd.rest.domain.ModuleMetadataResource;
import org.springframework.xd.rest.domain.RESTDeploymentState;
import org.springframework.xd.rest.domain.RESTModuleType;


/**
 * Knows how to assemble {@link ModuleMetadataResource}s out of {@link ModuleMetadata}s.
 *
 * @author Ilayaperumal Gopinathan
 */
public class ModuleMetadataResourceAssembler extends
		ResourceAssemblerSupport<ModuleMetadata, ModuleMetadataResource> {

	public ModuleMetadataResourceAssembler() {
		super(ModulesMetadataController.class, ModuleMetadataResource.class);
	}

	@Override
	public ModuleMetadataResource toResource(ModuleMetadata entity) {
		return createResourceWithId(entity.getQualifiedId(), entity);
	}

	@Override
	protected ModuleMetadataResource instantiateResource(ModuleMetadata entity) {
		String moduleType = (entity.getModuleType() != null) ? entity.getModuleType().name() : null;
		RESTDeploymentState deploymentStatus = (entity.getDeploymentStatus() != null) ? RESTDeploymentState.valueOf(entity.getDeploymentStatus().name())
				: null;
		return new ModuleMetadataResource(entity.getQualifiedId(), entity.getName(), entity.getUnitName(),
				RESTModuleType.valueOf(moduleType), entity.getContainerId(), entity.getModuleOptions(),
				entity.getDeploymentProperties(), deploymentStatus);
	}

}
