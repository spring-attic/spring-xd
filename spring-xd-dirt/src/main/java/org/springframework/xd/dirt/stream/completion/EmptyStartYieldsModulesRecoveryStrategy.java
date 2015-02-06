/*
 * Copyright 2013-2015 the original author or authors.
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
import static org.springframework.xd.module.ModuleType.sink;
import static org.springframework.xd.module.ModuleType.source;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.xd.dirt.module.ModuleRegistry;
import org.springframework.xd.dirt.stream.XDParser;
import org.springframework.xd.dirt.stream.dsl.CheckpointedStreamDefinitionException;
import org.springframework.xd.dirt.stream.dsl.TokenKind;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.rest.domain.CompletionKind;

/**
 * Proposes module names when the user has either<ul>
 * <li>typed nothing</li>
 * <li>typed a channel name followed by a ">"</li>
 * </ul>
 *
 * @author Eric Bottard
 */
@Component
public class EmptyStartYieldsModulesRecoveryStrategy extends
		StacktraceFingerprintingCompletionRecoveryStrategy<CheckpointedStreamDefinitionException> {

	private ModuleRegistry moduleRegistry;

	/**
	 * Construct a new EmptyStartYieldsModulesRecoveryStrategy given the parser and ModuleDefinition repository.
	 *
	 * @param parser the parser used to parse the text the partial module definition.
	 * @param moduleRegistry the registry to use for looking up all modules of a given type.
	 */
	@Autowired
	public EmptyStartYieldsModulesRecoveryStrategy(XDParser parser,
			ModuleRegistry moduleRegistry) {
		super(parser, CheckpointedStreamDefinitionException.class, "", "queue:foo >");
		this.moduleRegistry = moduleRegistry;
	}

	@Override
	@SuppressWarnings("fallthrough")
	public void addProposals(String dsl, CheckpointedStreamDefinitionException exception, CompletionKind kind, int detailLevel,
			List<String> proposals) {
		if ("".equals(dsl)) {
			switch (kind) {
				case module:
					// Add processors
					addAllModulesOfType(proposals, exception.getExpressionStringUntilCheckpoint(), processor);//NOSONAR
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
		} // There *was* some text, make sure it was a channel prefix
		else if (exception.getTokenPointer() > 0) {
			Assert.isTrue(exception.getTokens().get(exception.getTokenPointer() - 1).getKind() == TokenKind.GT);
			switch (kind) {
				case stream:
					addAllModulesOfType(proposals, dsl, processor);
					addAllModulesOfType(proposals, dsl, sink);
					break;
				case job:
				case module:
				default:
					break;
			}
		}

	}

	private void addAllModulesOfType(List<String> results, String start, ModuleType type) {
		String beginning = start.length() == 0 || start.endsWith(" ") ? start : start + " ";
		List<ModuleDefinition> mods = moduleRegistry.findDefinitions(type);
		for (ModuleDefinition mod : mods) {
			results.add(beginning + mod.getName());
		}
	}

}
