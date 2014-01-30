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

import static org.springframework.xd.dirt.stream.dsl.TokenKind.IDENTIFIER;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.xd.dirt.module.ModuleDefinitionRepository;
import org.springframework.xd.dirt.module.ModuleDeploymentRequest;
import org.springframework.xd.dirt.stream.XDParser;
import org.springframework.xd.dirt.stream.dsl.CheckpointedStreamDefinitionException;
import org.springframework.xd.dirt.stream.dsl.Token;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.options.ModuleOption;
import org.springframework.xd.module.options.ModuleOptionsMetadataResolver;
import org.springframework.xd.rest.client.domain.CompletionKind;

/**
 * Provides completions for the case where the user has started to type a module option name but it is not typed in full
 * yet.
 * 
 * @author Eric Bottard
 */
@Component
public class UnfinishedOptionNameRecoveryStrategy extends
		StacktraceFingerprintingCompletionRecoveryStrategy<CheckpointedStreamDefinitionException> {

	private ModuleDefinitionRepository moduleDefinitionRepository;

	private ModuleOptionsMetadataResolver moduleOptionsMetadataResolver;

	@Autowired
	public UnfinishedOptionNameRecoveryStrategy(XDParser parser, ModuleDefinitionRepository moduleDefinitionRepository,
			ModuleOptionsMetadataResolver moduleOptionsMetadataResolver) {
		super(parser, "file --dir=foo --pa", "file --pa", "file | filter | transform --expr");
		this.moduleDefinitionRepository = moduleDefinitionRepository;
		this.moduleOptionsMetadataResolver = moduleOptionsMetadataResolver;
	}

	@Override
	public void addProposals(CheckpointedStreamDefinitionException exception, CompletionKind kind, List<String> proposals) {
		String safe = exception.getExpressionStringUntilCheckpoint();
		List<Token> tokens = exception.getTokens();
		Token lastToken = tokens.get(tokens.size() - 1);
		Assert.isTrue(lastToken.isKind(IDENTIFIER));

		String optionNamePrefix = lastToken.stringValue();
		List<ModuleDeploymentRequest> parsed = parser.parse("dummy", safe);

		// List is in reverse order
		ModuleDeploymentRequest lastModule = parsed.get(0);
		String lastModuleName = lastModule.getModule();
		ModuleType lastModuleType = lastModule.getType();
		ModuleDefinition lastModuleDefinition = moduleDefinitionRepository.findByNameAndType(lastModuleName,
				lastModuleType);

		Set<String> alreadyPresentOptions = new HashSet<String>(lastModule.getParameters().keySet());
		for (ModuleOption option : moduleOptionsMetadataResolver.resolve(lastModuleDefinition)) {
			if (!alreadyPresentOptions.contains(option.getName()) && option.getName().startsWith(optionNamePrefix)) {
				proposals.add(String.format("%s --%s=", safe, option.getName()));
			}
		}

	}

}
