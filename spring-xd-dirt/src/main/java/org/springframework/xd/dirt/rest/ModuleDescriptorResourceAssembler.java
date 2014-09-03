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
import org.springframework.xd.module.ModuleDescriptor;
import org.springframework.xd.rest.domain.ModuleDescriptorResource;


/**
 * Knows how to assemble {@link ModuleDescriptorResource}s out of {@link ModuleMetadata}s.
 * 
 * @author Ilayaperumal Gopinathan
 */
public class ModuleDescriptorResourceAssembler extends
		ResourceAssemblerSupport<ModuleDescriptor, ModuleDescriptorResource> {

	public ModuleDescriptorResourceAssembler() {
		super(StreamConfigParserController.class, ModuleDescriptorResource.class);
	}

	@Override
	public ModuleDescriptorResource toResource(ModuleDescriptor entity) {
		return createResourceWithId(entity.getModuleLabel(), entity);
	}

	@Override
	protected ModuleDescriptorResource instantiateResource(ModuleDescriptor entity) {
		return new ModuleDescriptorResource(entity.getModuleLabel(), entity.getType().toString());
	}

}
