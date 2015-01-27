/*
 * Copyright 2014-2015 the original author or authors.
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
import org.springframework.stereotype.Component;
import org.springframework.xd.dirt.module.ModuleRegistry;
import org.springframework.xd.dirt.module.NoSuchModuleException;
import org.springframework.xd.dirt.stream.XDParser;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.rest.domain.CompletionKind;


/**
 * Provides completions by finding modules whose name starts with a prefix (which was assumed to be a correct module
 * name, but wasn't).
 * 
 * @author Eric Bottard
 */
@Component
public class UnfinishedModuleNameRecoveryStrategy extends
		StacktraceFingerprintingCompletionRecoveryStrategy<NoSuchModuleException> {

	private final ModuleRegistry moduleRegistry;

	/**
	 * Construct a new UnfinishedModuleNameRecoveryStrategy given the parser
	 * 
	 * @param parser the parser used to parse the text the partial module definition.
	 * @param moduleRegistry the registry to use for looking up all modules that start with a given name
	 *        prefix.
	 */
	@Autowired
	public UnfinishedModuleNameRecoveryStrategy(XDParser parser, ModuleRegistry moduleRegistry) {
		super(parser, NoSuchModuleException.class, "ht", "file | brid", "file | bridge | jd", "queue:foo > bar");
		this.moduleRegistry = moduleRegistry;
	}

	@Override
	public void addProposals(String dsl, NoSuchModuleException exception, CompletionKind kind, int detailLevel,
			List<String> proposals) {
		String modulePrefix = exception.getName();
		int index = dsl.lastIndexOf(modulePrefix);
		String chopped = dsl.substring(0, index);
		for (ModuleType expected : exception.getCandidateTypes()) {
			addModulesOfTypeWithPrefix(chopped, modulePrefix, expected, proposals);
		}
	}

	private void addModulesOfTypeWithPrefix(String beginning, String modulePrefix, ModuleType type, List<String> results) {
		List<ModuleDefinition> mods = moduleRegistry.findDefinitions(type);
		for (ModuleDefinition mod : mods) {
			if (mod.getName().startsWith(modulePrefix)) {
				results.add(beginning + mod.getName());
			}
		}
	}


}
