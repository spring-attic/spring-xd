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

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.xd.dirt.stream.XDParser;
import org.springframework.xd.dirt.stream.dsl.StreamDefinitionException;
import org.springframework.xd.rest.domain.CompletionKind;

/**
 * Provides completion when the user has typed in the first dash to a module option.
 * 
 * @author Eric Bottard
 */
@Component
public class ExpandOneDashToTwoDashesRecoveryStrategy extends
		StacktraceFingerprintingCompletionRecoveryStrategy<StreamDefinitionException> {


	@Autowired
	// Need field injection to prevent circular dependency
	private CompletionProvider completionProvider;

	/**
	 * Construct a new ExpandOneDashToTwoDashesRecoveryStrategy given the parser.
	 * 
	 * @param parser The parser used to parse the text the user has already typed in.
	 */
	@Autowired
	public ExpandOneDashToTwoDashesRecoveryStrategy(XDParser parser) {
		super(parser, StreamDefinitionException.class, "file -");
	}

	@Override
	public void addProposals(String dsl, StreamDefinitionException exception, CompletionKind kind, int detailLevel,
			List<String> proposals) {
		// Pretend there was an additional dash and invoke recursively
		List<String> completions = completionProvider.complete(kind, dsl + "-", detailLevel);
		proposals.addAll(completions);
	}


}
