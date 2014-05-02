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

import static org.springframework.xd.module.ModuleType.processor;
import static org.springframework.xd.module.ModuleType.sink;
import static org.springframework.xd.module.ModuleType.source;
import static org.springframework.xd.rest.client.domain.CompletionKind.module;
import static org.springframework.xd.rest.client.domain.CompletionKind.stream;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.xd.dirt.module.ModuleDefinitionRepository;
import org.springframework.xd.dirt.module.ModuleDescriptor;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.rest.client.domain.CompletionKind;

/**
 * Continues a well-formed stream definition by adding a pipe symbol and another module, provided that the stream
 * definition hasn't reached its end yet.
 * 
 * @author Eric Bottard
 */
@Component
public class PipeIntoOtherModulesExpansionStrategy implements CompletionExpansionStrategy {


	private ModuleDefinitionRepository moduleDefinitionRepository;

	/**
	 * Construct a new PipeIntoOtherModulesExpansionStrategy given a ModuleDefinition repository.
	 * 
	 * @param moduleDefinitionRepository the repository to check for the existence of the last entered module
	 *        definition.
	 */
	@Autowired
	public PipeIntoOtherModulesExpansionStrategy(ModuleDefinitionRepository moduleDefinitionRepository) {
		this.moduleDefinitionRepository = moduleDefinitionRepository;
	}

	@Override
	public boolean shouldTrigger(String text, List<ModuleDescriptor> parseResult, CompletionKind kind) {
		return true;
	}

	@Override
	public void addProposals(String start, List<ModuleDescriptor> parseResult, CompletionKind kind,
			List<String> proposals) {
		// List is in reverse order
		ModuleDescriptor lastModule = parseResult.get(0);
		ModuleType lastModuleType = lastModule.getType();

		// For full streams, add processors and sinks
		if (kind == stream && lastModuleType != ModuleType.sink) {
			addAllModulesOfType(start.endsWith(" ") ? start + "| " : start + " | ", processor, proposals);
			addAllModulesOfType(start.endsWith(" ") ? start + "| " : start + " | ", sink, proposals);
		}

		// For composed modules, don't go up to sink if we started with a source
		ModuleDescriptor firstModule = parseResult.get(parseResult.size() - 1);
		ModuleType firstModuleType = firstModule.getType();
		if (kind == module && lastModuleType != ModuleType.sink) {
			addAllModulesOfType(start.endsWith(" ") ? start + "| " : start + " | ", processor, proposals);
			if (firstModuleType != source) {
				addAllModulesOfType(start.endsWith(" ") ? start + "| " : start + " | ", sink, proposals);
			}
		}


	}

	private void addAllModulesOfType(String beginning, ModuleType type, List<String> results) {
		Page<ModuleDefinition> mods = moduleDefinitionRepository.findByType(new PageRequest(0, 1000), type);
		for (ModuleDefinition mod : mods) {
			results.add(beginning + mod.getName());
		}
	}


}
