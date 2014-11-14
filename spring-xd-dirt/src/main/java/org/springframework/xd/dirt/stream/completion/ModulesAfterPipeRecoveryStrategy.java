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

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.xd.dirt.module.ModuleRegistry;
import org.springframework.xd.dirt.stream.XDParser;
import org.springframework.xd.dirt.stream.dsl.CheckpointedStreamDefinitionException;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.rest.domain.CompletionKind;

/**
 * Provides completions for the case where the user has entered a pipe symbol and a module reference is expected next.
 *
 * @author Eric Bottard
 */
@Component
public class ModulesAfterPipeRecoveryStrategy extends
		StacktraceFingerprintingCompletionRecoveryStrategy<CheckpointedStreamDefinitionException> {


	private final ModuleRegistry moduleRegistry;

	/**
	 * Construct a new ExpandOneDashToTwoDashesRecoveryStrategy given the parser.
	 *
	 * @param parser the parser used to parse the text the partial module definition.
	 * @param moduleRegistry the registry to use for looking up all processors or sinks that can be added
	 *        after the pipe symbol.
	 */
	@Autowired
	public ModulesAfterPipeRecoveryStrategy(XDParser parser, ModuleRegistry moduleRegistry) {
		super(parser, CheckpointedStreamDefinitionException.class, "file | filter |");
		this.moduleRegistry = moduleRegistry;
	}

	@Override
	public void addProposals(String start, CheckpointedStreamDefinitionException exception, CompletionKind kind,
			int detailLevel, List<String> proposals) {

		addAllModulesOfType(start.endsWith(" ") ? start : start + " ", processor, proposals);
		addAllModulesOfType(start.endsWith(" ") ? start : start + " ", sink, proposals);
	}

	private void addAllModulesOfType(String beginning, ModuleType type, List<String> results) {
		List<ModuleDefinition> mods = moduleRegistry.findDefinitions(type);
		for (ModuleDefinition mod : mods) {
			results.add(beginning + mod.getName());
		}
	}

}
