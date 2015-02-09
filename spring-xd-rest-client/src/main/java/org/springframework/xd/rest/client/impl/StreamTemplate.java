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

import java.util.Collections;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.xd.rest.client.StreamOperations;
import org.springframework.xd.rest.domain.StreamDefinitionResource;

/**
 * Implementation of the Stream-related part of the API.
 *
 * @author Eric Bottard
 * @author Ilayaperumal Gopinathan
 */
public class StreamTemplate extends AbstractTemplate implements StreamOperations {

	StreamTemplate(AbstractTemplate source) {
		super(source);
	}

	@Override
	public StreamDefinitionResource createStream(String name, String definition, boolean deploy) {
		MultiValueMap<String, Object> values = new LinkedMultiValueMap<String, Object>();
		values.add("name", name);
		values.add("definition", definition);
		values.add("deploy", Boolean.toString(deploy));

		StreamDefinitionResource stream = restTemplate.postForObject(resources.get("streams/definitions").expand(),
				values,
				StreamDefinitionResource.class);
		return stream;
	}

	@Override
	public void destroy(String name) {
		// TODO: discover link by some other means (search by exact name on
		// /streams??)
		String uriTemplate = resources.get("streams/definitions").toString() + "/{name}";
		restTemplate.delete(uriTemplate, Collections.singletonMap("name", name));
	}

	@Override
	public void deploy(String name, String properties) {
		// TODO: discover link by some other means (search by exact name on
		// /streams??)
		String uriTemplate = resources.get("streams/deployments").toString() + "/{name}";
		MultiValueMap<String, Object> values = new LinkedMultiValueMap<String, Object>();
		if (properties != null) {
			values.add("properties", properties);
		}
		//TODO: Do we need StreamDeploymentResource? 
		restTemplate.postForObject(uriTemplate, values, Object.class, name);
	}

	@Override
	public void undeploy(String name) {
		// TODO: discover link by some other means (search by exact name on
		// /streams??)
		String uriTemplate = resources.get("streams/deployments").toString() + "/{name}";
		restTemplate.delete(uriTemplate, name);

	}

	@Override
	public StreamDefinitionResource.Page list() {
		String uriTemplate = resources.get("streams/definitions").toString();
		uriTemplate = uriTemplate + "?size=10000";
		return restTemplate.getForObject(uriTemplate, StreamDefinitionResource.Page.class);
	}

	@Override
	public void undeployAll() {
		restTemplate.delete(resources.get("streams/deployments").expand());
	}

	@Override
	public void destroyAll() {
		restTemplate.delete(resources.get("streams/definitions").expand());
	}

}
