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
import org.springframework.xd.dirt.module.ModuleDeploymentRequest;
import org.springframework.xd.dirt.stream.XDParser;
import org.springframework.xd.rest.client.domain.CompletionKind;

/**
 * Provides code completion on a (maybe ill-formed) stream definition.
 * 
 * @author Eric Bottard
 */
@Service
// Lazy needed to prevent circular dependency with ExpandOneDashToTwoDashesRecoveryStrategy
@Lazy
public class CompletionProvider {

	private final XDParser parser;

	private final List<CompletionRecoveryStrategy<Throwable>> recoveries;

	private final List<CompletionExpansionStrategy> expansions;


	@Autowired
	@SuppressWarnings("unchecked")
	public CompletionProvider(XDParser parser,
			List<CompletionRecoveryStrategy<? extends Throwable>> recoveries,
			List<CompletionExpansionStrategy> expansions) {
		this.parser = parser;
		// Unchecked downcast here is the best compromise
		// if we want to still benefit from Spring's typed collection injection
		Object o = recoveries;
		this.recoveries = (List<CompletionRecoveryStrategy<Throwable>>) o;
		this.expansions = expansions;
	}


	/*
	 * Attempt to parse the text the user has already typed in. This either succeeds, in which case we may propose to
	 * expand what she has typed, or it fails (most likely because this is not well formed), in which case we try to
	 * recover from the parsing failure and still add proposals.
	 */
	public List<String> complete(CompletionKind kind, String start) {
		List<String> results = new ArrayList<String>();

		String name = "__dummy";
		List<ModuleDeploymentRequest> parsed = null;
		try {
			parsed = parser.parse(name, start);
		}
		catch (Throwable recoverable) {
			for (CompletionRecoveryStrategy<Throwable> strategy : recoveries) {
				if (strategy.shouldTrigger(recoverable, kind)) {
					strategy.addProposals(recoverable, kind, results);
				}
			}

			return results;
		}

		for (CompletionExpansionStrategy strategy : expansions) {
			if (strategy.shouldTrigger(start, parsed, kind)) {
				strategy.addProposals(start, parsed, kind, results);
			}
		}
		return results;


	}


}
