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

package org.springframework.xd.dirt.stream;

import static org.springframework.xd.dirt.stream.CompletionKind.stream;
import static org.springframework.xd.module.ModuleType.processor;
import static org.springframework.xd.module.ModuleType.sink;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.xd.dirt.module.ModuleDefinitionRepository;
import org.springframework.xd.dirt.module.ModuleDeploymentRequest;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.options.ModuleOption;


public class CompletionProvider {

	private final XDParser parser;

	private final ModuleDefinitionRepository moduleDefinitionRepository;


	@Autowired
	public CompletionProvider(XDParser parser, ModuleDefinitionRepository moduleDefinitionRepository) {
		this.parser = parser;
		this.moduleDefinitionRepository = moduleDefinitionRepository;
	}


	public List<String> complete(CompletionKind kind, String start) {
		List<String> result = new ArrayList<String>();

		if (start.trim().equals("")) {
			addAllModulesOfType(ModuleType.source, result, start);
			return result;
		}

		String name = "dummy";
		List<ModuleDeploymentRequest> parsed = parser.parse(name, start);
		// List is in reverse order
		ModuleDeploymentRequest lastModule = parsed.get(0);
		String lastModuleName = lastModule.getModule();
		ModuleType lastModuleType = lastModule.getType();
		ModuleDefinition lastModuleDefinition = moduleDefinitionRepository.findByNameAndType(lastModuleName,
				lastModuleType);
		Set<String> alreadyPresentOptions = new HashSet<String>(lastModule.getParameters().keySet());
		for (ModuleOption option : lastModuleDefinition.getModuleOptionsMetadata()) {
			if (!alreadyPresentOptions.contains(option.getName())) {
				result.add(start + String.format(" --%s=", option.getName()));
			}
		}

		if (lastModuleType != ModuleType.sink && kind == stream) {
			addAllModulesOfType(processor, result, start + " | ");
			addAllModulesOfType(sink, result, start + " | ");
		}


		return result;
	}

	private void addAllModulesOfType(ModuleType type, List<String> results, String start) {
		Page<ModuleDefinition> mods = moduleDefinitionRepository.findByType(new PageRequest(0, 1000), type);
		for (ModuleDefinition mod : mods) {
			results.add(start + mod.getName());
		}
	}

}
