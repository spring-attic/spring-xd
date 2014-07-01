/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.xd.dirt.stream.completion;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.xd.dirt.module.ModuleDefinitionRepository;
import org.springframework.xd.dirt.module.NoSuchModuleException;
import org.springframework.xd.dirt.stream.XDParser;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.rest.client.domain.CompletionKind;


/**
 * Provides completions by finding modules whose name starts with a prefix (which was assumed to be a correct module
 * name, but wasn't).
 * 
 * @author Eric Bottard
 */
@Component
public class UnfinishedModuleNameRecoveryStrategy extends
		StacktraceFingerprintingCompletionRecoveryStrategy<NoSuchModuleException> {

	private final ModuleDefinitionRepository moduleDefinitionRepository;

	/**
	 * Construct a new UnfinishedModuleNameRecoveryStrategy given the parser
	 * 
	 * @param parser the parser used to parse the text the partial module definition.
	 * @param moduleDefinitionRepository the repository to use for looking up all modules that start with a given name
	 *        prefix.
	 */
	@Autowired
	public UnfinishedModuleNameRecoveryStrategy(XDParser parser, ModuleDefinitionRepository moduleDefinitionRepository) {
		super(parser, NoSuchModuleException.class, "ht", "file | brid", "file | bridge | jd", "queue:foo > bar");
		this.moduleDefinitionRepository = moduleDefinitionRepository;
	}

	@Override
	public void addProposals(String dsl, NoSuchModuleException exception, CompletionKind kind, int lod,
			List<String> proposals) {
		String modulePrefix = exception.getName();
		int index = dsl.lastIndexOf(modulePrefix);
		String chopped = dsl.substring(0, index);
		for (ModuleType expected : exception.getCandidateTypes()) {
			addModulesOfTypeWithPrefix(chopped, modulePrefix, expected, proposals);
		}
	}

	private void addModulesOfTypeWithPrefix(String beginning, String modulePrefix, ModuleType type, List<String> results) {
		Page<ModuleDefinition> mods = moduleDefinitionRepository.findByType(new PageRequest(0, 1000), type);
		for (ModuleDefinition mod : mods) {
			if (mod.getName().startsWith(modulePrefix)) {
				results.add(beginning + mod.getName());
			}
		}
	}


}
