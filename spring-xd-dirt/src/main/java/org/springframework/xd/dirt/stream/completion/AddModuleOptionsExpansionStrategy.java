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

package org.springframework.xd.dirt.stream.completion;

import static org.springframework.xd.dirt.stream.completion.CompletionProvider.shouldShowOption;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.xd.dirt.module.ModuleRegistry;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleDescriptor;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.options.ModuleOption;
import org.springframework.xd.module.options.ModuleOptionsMetadataResolver;
import org.springframework.xd.rest.domain.CompletionKind;

/**
 * Adds missing module options at the end of a well formed stream definition.
 *
 * @author Eric Bottard
 */
@Component
public class AddModuleOptionsExpansionStrategy implements CompletionExpansionStrategy {


	private ModuleRegistry moduleRegistry;

	private ModuleOptionsMetadataResolver moduleOptionsMetadataResolver;

	/**
	 * Construct a new AddModuleOptionsExpansionStrategy for use in detecting missing module options.
	 *
	 * @param moduleRegistry the registry to check for the existence of the last entered module
	 *        definition.
	 * @param moduleOptionsMetadataResolver the metadata resolver to use in order to create a list of proposals for
	 *        module options that have not yet been specified.
	 */
	@Autowired
	public AddModuleOptionsExpansionStrategy(ModuleRegistry moduleRegistry,
			ModuleOptionsMetadataResolver moduleOptionsMetadataResolver) {
		this.moduleRegistry = moduleRegistry;
		this.moduleOptionsMetadataResolver = moduleOptionsMetadataResolver;
	}

	@Override
	public boolean shouldTrigger(String text, List<ModuleDescriptor> parseResult, CompletionKind kind) {
		return true;
	}

	@Override
	public void addProposals(String text, List<ModuleDescriptor> parseResult, CompletionKind kind, int detailLevel,
			List<String> proposals) {
		// List is in reverse order
		ModuleDescriptor lastModule = parseResult.get(0);
		String lastModuleName = lastModule.getModuleName();
		ModuleType lastModuleType = lastModule.getType();
		ModuleDefinition lastModuleDefinition = moduleRegistry.findDefinition(lastModuleName, lastModuleType);

		Set<String> alreadyPresentOptions = new HashSet<String>(lastModule.getParameters().keySet());
		for (ModuleOption option : moduleOptionsMetadataResolver.resolve(lastModuleDefinition)) {
			if (shouldShowOption(option, detailLevel) && !alreadyPresentOptions.contains(option.getName())) {
				proposals.add(String.format("%s%s--%s=", text, text.endsWith(" ") ? "" : " ", option.getName()));
			}
		}

	}

}
