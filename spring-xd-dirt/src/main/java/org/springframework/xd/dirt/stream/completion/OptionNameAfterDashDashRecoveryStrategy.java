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

import static org.springframework.xd.dirt.stream.completion.CompletionProvider.shouldShowOption;
import static org.springframework.xd.dirt.stream.completion.CompletionProvider.toParsingContext;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.xd.dirt.module.ModuleRegistry;
import org.springframework.xd.dirt.stream.XDParser;
import org.springframework.xd.dirt.stream.dsl.CheckpointedStreamDefinitionException;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleDescriptor;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.core.CompositeModule;
import org.springframework.xd.module.options.ModuleOption;
import org.springframework.xd.module.options.ModuleOptionsMetadataResolver;
import org.springframework.xd.rest.domain.CompletionKind;

/**
 * Provides completion proposals when the user has typed the two dashes that precede a module option name.
 *
 * @author Eric Bottard
 */
@Component
public class OptionNameAfterDashDashRecoveryStrategy extends
		StacktraceFingerprintingCompletionRecoveryStrategy<CheckpointedStreamDefinitionException> {

	private ModuleRegistry moduleRegistry;

	private ModuleOptionsMetadataResolver moduleOptionsMetadataResolver;


	/**
	 * Construct a new ExpandOneDashToTwoDashesRecoveryStrategy given the parser,
	 *
	 * @param parser the parser used to parse the text the partial module definition.
	 * @param moduleRegistry the registry to check for the existence of the last entered module
	 *        definition.
	 * @param moduleOptionsMetadataResolver the metadata resolver to use in order to create a list of proposals for
	 *        module options that have not yet been specified.
	 */
	@Autowired
	public OptionNameAfterDashDashRecoveryStrategy(XDParser parser,
			ModuleRegistry moduleRegistry,
			ModuleOptionsMetadataResolver moduleOptionsMetadataResolver) {
		super(parser, CheckpointedStreamDefinitionException.class, "file --dir=foo --", "file --",
				"file | filter | transform --");
		this.moduleRegistry = moduleRegistry;
		this.moduleOptionsMetadataResolver = moduleOptionsMetadataResolver;
	}

	@Override
	public void addProposals(String dsl, CheckpointedStreamDefinitionException exception, CompletionKind kind, int detailLevel,
			List<String> proposals) {
		String safe = exception.getExpressionStringUntilCheckpoint();
		List<ModuleDescriptor> parsed = parser.parse("__dummy", safe, toParsingContext(kind));

		// List is in reverse order
		ModuleDescriptor lastModule = parsed.get(0);
		String lastModuleName = lastModule.getModuleName();
		ModuleType lastModuleType = lastModule.getType();
		ModuleDefinition lastModuleDefinition = moduleRegistry.findDefinition(lastModuleName, lastModuleType);

		Set<String> alreadyPresentOptions = new HashSet<String>(lastModule.getParameters().keySet().size());
		// If we're providing completions for a composed module, some options
		// may already have a value appearing from the definition of the module itself, yet
		// the user *may* want to override them anyways. So pretend they're not there
		for (String optionName : lastModule.getParameters().keySet()) {
			if (!optionName.contains(CompositeModule.OPTION_SEPARATOR)) {
				alreadyPresentOptions.add(optionName);
			}
		}
		for (ModuleOption option : moduleOptionsMetadataResolver.resolve(lastModuleDefinition)) {
			if (shouldShowOption(option, detailLevel) && !alreadyPresentOptions.contains(option.getName())) {
				proposals.add(String.format("%s --%s=", safe, option.getName()));
			}
		}

	}

}
