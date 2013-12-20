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
import static org.springframework.xd.rest.client.domain.CompletionKind.stream;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.xd.dirt.module.ModuleDefinitionRepository;
import org.springframework.xd.dirt.module.ModuleDeploymentRequest;
import org.springframework.xd.dirt.stream.XDParser;
import org.springframework.xd.dirt.stream.dsl.CheckpointedStreamDefinitionException;
import org.springframework.xd.dirt.stream.dsl.StreamDefinitionException;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.options.ModuleOption;
import org.springframework.xd.rest.client.domain.CompletionKind;


public class CompletionProvider {

	private final XDParser parser;

	private final ModuleDefinitionRepository moduleDefinitionRepository;

	private final List<StacktraceFingerprintlingCompletionRecoveryStrategy> recoveries = new ArrayList<StacktraceFingerprintlingCompletionRecoveryStrategy>();


	@Autowired
	public CompletionProvider(XDParser parser, ModuleDefinitionRepository moduleDefinitionRepository) {
		this.parser = parser;
		this.moduleDefinitionRepository = moduleDefinitionRepository;
		recoveries.add(new ModulesAfterPipeRecoveryStrategy(parser, moduleDefinitionRepository));
		recoveries.add(new OptionNameAfterDashDashRecoveryStrategy(parser, moduleDefinitionRepository));
		recoveries.add(new UnfinishedOptionNameRecoveryStrategy(parser, moduleDefinitionRepository));
	}


	private class CompletionProposals {

		private final CompletionKind kind;

		private String start;

		private List<String> results = new ArrayList<String>();

		public CompletionProposals(CompletionKind kind, String start) {
			this.kind = kind;
			this.start = start;
		}

		private void compute() {
			if (start.trim().equals("")) {
				addAllModulesOfType(start, ModuleType.source);
				return;
			}

			String name = "dummy";
			List<ModuleDeploymentRequest> parsed = null;
			try {
				parsed = parser.parse(name, start);
			}
			catch (CheckpointedStreamDefinitionException recoverable) {
				for (StacktraceFingerprintlingCompletionRecoveryStrategy strategy : recoveries) {
					if (strategy.matches(recoverable)) {
						strategy.use(recoverable, results, kind);
						break;
					}
				}

				return;
			}
			catch (StreamDefinitionException e) {
				return;
			}
			// List is in reverse order
			ModuleDeploymentRequest lastModule = parsed.get(0);
			String lastModuleName = lastModule.getModule();
			ModuleType lastModuleType = lastModule.getType();
			ModuleDefinition lastModuleDefinition = moduleDefinitionRepository.findByNameAndType(lastModuleName,
					lastModuleType);

			Set<String> alreadyPresentOptions = new HashSet<String>(lastModule.getParameters().keySet());
			for (ModuleOption option : lastModuleDefinition.getModuleOptionsMetadata()) {
				if (!alreadyPresentOptions.contains(option.getName())) {
					continueWith(String.format("--%s=", option.getName()));
				}
			}

			if (lastModuleType != ModuleType.sink && kind == stream) {
				addAllModulesOfType(start + maybePrefixWithSpace("| "), processor);
				addAllModulesOfType(start + maybePrefixWithSpace("| "), sink);
			}

		}

		private void continueWith(String what) {
			results.add(start + (start.endsWith(" ") ? what : " " + what));
		}

		private void addAllModulesOfType(String beginning, ModuleType type) {
			Page<ModuleDefinition> mods = moduleDefinitionRepository.findByType(new PageRequest(0, 1000), type);
			for (ModuleDefinition mod : mods) {
				results.add(beginning + mod.getName());
			}
		}

		/**
		 * Add an extra space before what if it is not already present at the end of what the user already typed.
		 */
		private String maybePrefixWithSpace(String what) {
			return start.endsWith(" ") ? what : " " + what;
		}


	}

	public List<String> complete(CompletionKind kind, String start) {
		CompletionProposals proposals = new CompletionProposals(kind, start);
		proposals.compute();
		return proposals.results;
	}


}
