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

package org.springframework.xd.rest.client.impl;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import org.springframework.hateoas.UriTemplate;
import org.springframework.xd.rest.client.CompletionOperations;
import org.springframework.xd.rest.domain.CompletionKind;

/**
 * Implements the {@link CompletionOperations} part of the API.
 * 
 * @author Eric Bottard
 */
public class CompletionTemplate extends AbstractTemplate implements CompletionOperations {


	CompletionTemplate(AbstractTemplate other) {
		super(other);
	}

	@Override
	public List<String> completions(CompletionKind kind, String start, int lod) {
		UriTemplate template = resources.get(String.format("completions/%s", kind));
		URI expanded = template.expand(start, lod);
		return Arrays.asList(restTemplate.getForObject(expanded, String[].class));
	}
}
