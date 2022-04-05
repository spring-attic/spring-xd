/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.dirt.stream.completion;

import java.util.List;

import org.springframework.xd.module.ModuleDescriptor;
import org.springframework.xd.rest.domain.CompletionKind;

/**
 * Used to enhance a well formed stream definition by adding yet more text to it (<i>e.g.</i> adding more options to a
 * module).
 * 
 * @author Eric Bottard
 */
public interface CompletionExpansionStrategy {

	/**
	 * Whether this completion should be triggered.
	 */
	boolean shouldTrigger(String text, List<ModuleDescriptor> parseResult, CompletionKind kind);

	/**
	 * Perform code completion by adding proposals to the {@code proposals} list.
	 */
	void addProposals(String text, List<ModuleDescriptor> parseResult, CompletionKind kind, int detailLevel,
			List<String> proposals);

}
