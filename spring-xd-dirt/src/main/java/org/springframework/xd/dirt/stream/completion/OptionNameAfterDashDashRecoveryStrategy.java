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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.xd.dirt.module.ModuleDefinitionRepository;
import org.springframework.xd.dirt.module.ModuleDeploymentRequest;
import org.springframework.xd.dirt.stream.XDParser;
import org.springframework.xd.dirt.stream.dsl.CheckpointedStreamDefinitionException;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.options.ModuleOption;
import org.springframework.xd.rest.client.domain.CompletionKind;


public class OptionNameAfterDashDashRecoveryStrategy extends
		StacktraceFingerprintlingCompletionRecoveryStrategy<CheckpointedStreamDefinitionException> {

	private ModuleDefinitionRepository moduleDefinitionRepository;

	public OptionNameAfterDashDashRecoveryStrategy(XDParser parser,
			ModuleDefinitionRepository moduleDefinitionRepository) {
		super(parser, "file --dir=foo --", "file --", "file | filter | transform --");
		this.moduleDefinitionRepository = moduleDefinitionRepository;
	}

	@Override
	public void use(CheckpointedStreamDefinitionException exception, List<String> result, CompletionKind kind) {
		String safe = exception.getExpressioStringUntilCheckpoint();
		List<ModuleDeploymentRequest> parsed = parser.parse("dummy", safe);

		// List is in reverse order
		ModuleDeploymentRequest lastModule = parsed.get(0);
		String lastModuleName = lastModule.getModule();
		ModuleType lastModuleType = lastModule.getType();
		ModuleDefinition lastModuleDefinition = moduleDefinitionRepository.findByNameAndType(lastModuleName,
				lastModuleType);

		Set<String> alreadyPresentOptions = new HashSet<String>(lastModule.getParameters().keySet());
		for (ModuleOption option : lastModuleDefinition.getModuleOptionsMetadata()) {
			if (!alreadyPresentOptions.contains(option.getName())) {
				result.add(String.format("%s --%s=", safe, option.getName()));
			}
		}


	}

}
