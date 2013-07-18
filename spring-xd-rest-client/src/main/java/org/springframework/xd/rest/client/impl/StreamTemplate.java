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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.xd.rest.client.StreamOperations;
import org.springframework.xd.rest.client.domain.StreamDefinitionResource;

/**
 * Implementation of the Stream-related part of the API.
 *
 * @author Eric Bottard
 */
public class StreamTemplate extends AbstractTemplate implements StreamOperations {

	StreamTemplate(AbstractTemplate source) {
		super(source);
	}

	@Override
	public StreamDefinitionResource createStream(String name, String defintion, boolean deploy) {
		MultiValueMap<String, Object> values = new LinkedMultiValueMap<String, Object>();
		values.add("name", name);
		values.add("definition", defintion);
		values.add("deploy", Boolean.toString(deploy));

		StreamDefinitionResource stream = restTemplate.postForObject(resources.get("streams"), values,
				StreamDefinitionResource.class);
		return stream;
	}

	@Override
	public void destroyStream(String name) {
		// TODO: discover link by some other means (search by exact name on
		// /streams??)
		String uriTemplate = resources.get("streams").toString() + "/{name}";
		restTemplate.delete(uriTemplate, Collections.singletonMap("name", name));
	}

	@Override
	public void deployStream(String name) {
		// TODO: discover link by some other means (search by exact name on
		// /streams??)
		String uriTemplate = resources.get("streams").toString() + "/{name}";
		MultiValueMap<String, Object> values = new LinkedMultiValueMap<String, Object>();
		values.add("deploy", "true");
		restTemplate.put(uriTemplate, values, name);
	}

	@Override
	public void undeployStream(String name) {
		// TODO: discover link by some other means (search by exact name on
		// /streams??)
		String uriTemplate = resources.get("streams").toString() + "/{name}";
		MultiValueMap<String, Object> values = new LinkedMultiValueMap<String, Object>();
		values.add("deploy", "false");
		restTemplate.put(uriTemplate, values, name);

	}

	@Override
	public/* PagedResources */List<StreamDefinitionResource> list() {
		String uriTemplate = resources.get("streams").toString();
		MultiValueMap<String, Object> values = new LinkedMultiValueMap<String, Object>();
		values.add("size", "10000");
		// PagedResources<StreamDefinitionResource> result = restTemplate.getForObject(uriTemplate,
		// PagedResources.class,
		// values);
		List<StreamDefinitionResource> result = Arrays.asList(restTemplate.getForObject(uriTemplate,
				StreamDefinitionResource[].class, values));
		return result;
	}

}
