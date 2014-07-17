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

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.xd.dirt.stream.ParsingContext;
import org.springframework.xd.dirt.stream.XDParser;
import org.springframework.xd.module.ModuleDescriptor;
import org.springframework.xd.module.options.ModuleOption;
import org.springframework.xd.rest.domain.CompletionKind;

/**
 * Provides code completion on a (maybe ill-formed) stream definition.
 * 
 * @author Eric Bottard
 */
@Service
// Lazy needed to prevent circular dependency with ExpandOneDashToTwoDashesRecoveryStrategy
@Lazy
public class CompletionProvider {

	/**
	 * The number of times (inclusive) the user must invoke completion for {@link ModuleOption#hidden(boolean) hidden}
	 * options to show up 
	 */
	private final static int HIDDEN_OPTION_THRESHOLD = 2;

	private final XDParser parser;

	private final List<CompletionRecoveryStrategy<Exception>> completionRecoveryStrategies;

	private final List<CompletionExpansionStrategy> completionExpansionStrategies;

	public static boolean shouldShowOption(ModuleOption option, int detailLevel) {
		return !option.isHidden() || detailLevel >= HIDDEN_OPTION_THRESHOLD;
	}


	/**
	 * Construct a new CompletionProvider given a list of recovery strategies and expansion strategies.
	 * 
	 * @param parser the parser used to parse the text the partial module definition.
	 * @param completionRecoveryStrategies list of strategies to apply when an exception was thrown during parsing.
	 * @param completionExpansionStrategies list of strategies to apply for adding additional module options completion
	 *        suggestions.
	 */
	@Autowired
	@SuppressWarnings("unchecked")
	public CompletionProvider(XDParser parser,
			List<CompletionRecoveryStrategy<? extends Exception>> completionRecoveryStrategies,
			List<CompletionExpansionStrategy> completionExpansionStrategies) {
		this.parser = parser;
		// Unchecked downcast here is the best compromise
		// if we want to still benefit from Spring's typed collection injection
		Object o = completionRecoveryStrategies;
		this.completionRecoveryStrategies = (List<CompletionRecoveryStrategy<Exception>>) o;
		this.completionExpansionStrategies = completionExpansionStrategies;
	}


	/*
	 * Attempt to parse the text the user has already typed in. This either succeeds, in which case we may propose to
	 * expand what she has typed, or it fails (most likely because this is not well formed), in which case we try to
	 * recover from the parsing failure and still add proposals.
	 */
	public List<String> complete(CompletionKind kind, String start, int detailLevel) {
		List<String> results = new ArrayList<String>();

		String name = "__dummy";
		List<ModuleDescriptor> parsed = null;
		try {
			parsed = parser.parse(name, start, toParsingContext(kind));
		}
		catch (Exception recoverable) {
			for (CompletionRecoveryStrategy<Exception> strategy : completionRecoveryStrategies) {
				if (strategy.shouldTrigger(start, recoverable, kind)) {
					strategy.addProposals(start, recoverable, kind, detailLevel, results);
				}
			}

			return results;
		}

		for (CompletionExpansionStrategy strategy : completionExpansionStrategies) {
			if (strategy.shouldTrigger(start, parsed, kind)) {
				strategy.addProposals(start, parsed, kind, detailLevel, results);
			}
		}
		return results;
	}

	static ParsingContext toParsingContext(CompletionKind kind) {
		switch (kind) {
			case stream:
				return ParsingContext.partial_stream;
			case module:
				return ParsingContext.partial_module;
			case job:
				return ParsingContext.partial_job;
			default:
				throw new IllegalArgumentException("Unknown kind: " + kind);
		}
	}


}
