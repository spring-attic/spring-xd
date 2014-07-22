/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.xd.dirt.rest;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.xd.dirt.stream.completion.CompletionProvider;
import org.springframework.xd.rest.domain.CompletionKind;

/**
 * Exposes completion for stream-like definitions as a REST endpoint.
 *
 * @author Eric Bottard
 */
@RestController
@RequestMapping("/completions")
@Lazy
public class CompletionsController {

	private final CompletionProvider completionProvider;

	@Autowired
	public CompletionsController(CompletionProvider completionProvider) {
		this.completionProvider = completionProvider;
	}

	/**
	 * Return a list of possible completions given a prefix string that the user has started typing.
	 *
	 *  @param kind the kind of definition that is being authored
	 *  @param start the amount of text written so far
	 *  @param detailLevel the level of detail the user wants in completions
	 *                     (<i>e.g.</i> higher numbers may mean show 'hidden' options)
	 */
	@RequestMapping(value = "/{kind}")
	public List<String> completions(@PathVariable("kind") CompletionKind kind,
			@RequestParam("start") String start,
			@RequestParam(value = "detailLevel", defaultValue = "1") int detailLevel) {
		return completionProvider.complete(kind, start, detailLevel);
	}
}
