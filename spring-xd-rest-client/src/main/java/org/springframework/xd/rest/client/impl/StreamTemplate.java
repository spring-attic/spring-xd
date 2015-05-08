/*
 * Copyright 2013-2015 the original author or authors.
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

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.xd.rest.client.StreamOperations;
import org.springframework.xd.rest.domain.StreamDefinitionResource;

/**
 * Implementation of the Stream-related part of the API.
 *
 * @author Eric Bottard
 * @author Ilayaperumal Gopinathan
 * @author Paul Harris
 * @author Gary Russell
 */
public class StreamTemplate extends AbstractResourceTemplate implements StreamOperations {

	StreamTemplate(AbstractTemplate source, String adminUri, String password, String username, String vhost,
			String busPrefix) {
		super(source, adminUri, password, "streams", username, vhost, busPrefix);
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
	public StreamDefinitionResource.Page list() {
		String uriTemplate = resources.get("streams/definitions").toString();
		uriTemplate = uriTemplate + "?size=10000";
		return restTemplate.getForObject(uriTemplate, StreamDefinitionResource.Page.class);
	}

}
