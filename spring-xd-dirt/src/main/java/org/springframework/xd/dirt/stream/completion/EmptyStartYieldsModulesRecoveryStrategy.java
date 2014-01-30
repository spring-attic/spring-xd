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

import static org.springframework.xd.module.ModuleType.job;
import static org.springframework.xd.module.ModuleType.processor;
import static org.springframework.xd.module.ModuleType.source;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.xd.dirt.module.ModuleDefinitionRepository;
import org.springframework.xd.dirt.stream.XDParser;
import org.springframework.xd.dirt.stream.dsl.CheckpointedStreamDefinitionException;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.rest.client.domain.CompletionKind;


@Component
public class EmptyStartYieldsModulesRecoveryStrategy extends
		StacktraceFingerprintingCompletionRecoveryStrategy<CheckpointedStreamDefinitionException> {

	private ModuleDefinitionRepository moduleDefinitionRepository;

	@Autowired
	public EmptyStartYieldsModulesRecoveryStrategy(XDParser parser,
			ModuleDefinitionRepository moduleDefinitionRepository) {
		super(parser, "");
		this.moduleDefinitionRepository = moduleDefinitionRepository;
	}

	@Override
	@SuppressWarnings("fallthrough")
	public void addProposals(CheckpointedStreamDefinitionException exception, CompletionKind kind,
			List<String> proposals) {
		switch (kind) {
			case module:
				// Add processors
				addAllModulesOfType(proposals, exception.getExpressionStringUntilCheckpoint(), processor);
				// FALL THRU as composed modules can be either
				// source | processors - or -
				// processors | sink
			case stream:
				// Add sources
				addAllModulesOfType(proposals, exception.getExpressionStringUntilCheckpoint(), source);
				break;
			case job:
				// Add jobs
				addAllModulesOfType(proposals, exception.getExpressionStringUntilCheckpoint(), job);
				break;

			default:
				break;
		}
	}

	private void addAllModulesOfType(List<String> results, String start, ModuleType type) {
		String beginning = start.length() == 0 || start.endsWith(" ") ? start : start + " ";
		Page<ModuleDefinition> mods = moduleDefinitionRepository.findByType(new PageRequest(0, 1000), type);
		for (ModuleDefinition mod : mods) {
			results.add(beginning + mod.getName());
		}
	}


}
