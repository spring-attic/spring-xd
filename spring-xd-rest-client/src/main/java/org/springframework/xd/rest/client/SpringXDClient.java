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

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.xd.rest.client.domain.Stream;

/**
 * Implementation of the SpringXD remote interaction API.
 * 
 * @author Eric Bottard
 */
public class SpringXDClient implements SpringXDOperations {

	private RestTemplate restTemplate = new RestTemplate();

	private Map<String, URI> resources = new HashMap<String, URI>();

	public SpringXDClient(URI baseURI) {

	}

	@Override
	public Stream deployStream(String name, String defintion) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromUri(resources
				.get("streams/deploy"));

		restTemplate.put(builder.buildAndExpand(name).toUriString(), defintion);
		return null;
	}

}
