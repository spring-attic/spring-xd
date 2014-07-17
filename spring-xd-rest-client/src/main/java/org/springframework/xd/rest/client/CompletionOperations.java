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

package org.springframework.xd.rest.client;

import java.util.List;

import org.springframework.xd.rest.domain.CompletionKind;

/**
 * Provides access to the dsl completion facility.
 * 
 * @author Eric Bottard
 */
public interface CompletionOperations {

	/**
	 * Return the list of completions that are compatible with the given DSL prefix.
	 * 
	 * @param levelOfDetail 1 based integer allowing progressive disclosure of more and more
	 * complex completions 
	 */
	List<String> completions(CompletionKind kind, String prefix, int levelOfDetail);

}
