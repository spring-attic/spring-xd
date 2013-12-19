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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import org.springframework.stereotype.Component;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.options.DefaultModuleOptionsMetadata;
import org.springframework.xd.module.options.ModuleOption;
import org.springframework.xd.module.options.ModuleOptionsMetadata;
import org.springframework.xd.module.options.ModuleOptionsMetadataResolver;
import org.springframework.xd.rest.client.domain.DetailedModuleDefinitionResource;


/**
 * Knows how to build {@link DetailedModuleDefinitionResource} out of a {@link ModuleDefinition}.
 * 
 * @author Eric Bottard
 */
@Component
public class DetailedModuleDefinitionResourceAssembler extends
		ResourceAssemblerSupport<ModuleDefinition, DetailedModuleDefinitionResource> {

	private ModuleOptionsMetadataResolver moduleOptionsMetadataResolver;

	@Autowired
	public DetailedModuleDefinitionResourceAssembler(ModuleOptionsMetadataResolver moduleOptionsMetadataResolver) {
		super(ModulesController.class, DetailedModuleDefinitionResource.class);
		this.moduleOptionsMetadataResolver = moduleOptionsMetadataResolver;
	}

	@Override
	public DetailedModuleDefinitionResource toResource(ModuleDefinition entity) {
		return createResourceWithId(entity.getType() + "/" + entity.getName(), entity);
	}

	@Override
	protected DetailedModuleDefinitionResource instantiateResource(ModuleDefinition entity) {
		DetailedModuleDefinitionResource result = new DetailedModuleDefinitionResource(entity.getName(),
				entity.getType().name());
		ModuleOptionsMetadata moduleOptionsMetadata = moduleOptionsMetadataResolver.resolve(entity);

		if (!(moduleOptionsMetadata instanceof DefaultModuleOptionsMetadata)) {
			for (ModuleOption option : moduleOptionsMetadata) {
				Object defaultValue = option.getDefaultValue();
				Class<?> type = option.getType();
				result.addOption(new DetailedModuleDefinitionResource.Option(option.getName(),
						type == null ? null : type.getSimpleName(), option.getDescription(),
						defaultValue == null ? null : defaultValue.toString()));
			}
		}
		return result;
	}

}
