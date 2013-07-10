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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.xd.rest.client.domain.StreamDefinitionResource;
import org.springframework.xd.rest.client.domain.TapDefinitionResource;
import org.springframework.xd.rest.client.domain.XDRuntime;

/**
 * Implementation of the SpringXD remote interaction API.
 *
 * @author Eric Bottard
 * @author Ilayaperumal Gopinathan
 */
public class SpringXDClient implements SpringXDOperations {

	private RestTemplate restTemplate = new RestTemplate();

	private Map<String, URI> resources = new HashMap<String, URI>();

	public SpringXDClient(URI baseURI) {
		XDRuntime xdRuntime = restTemplate.getForObject(baseURI,
				XDRuntime.class);
		resources.put("streams",
				URI.create(xdRuntime.getLink("streams").getHref()));
		resources.put("taps",
				URI.create(xdRuntime.getLink("taps").getHref()));

	}

	@Override
	public StreamDefinitionResource deployStream(String name, String defintion) {
		MultiValueMap<String, Object> values = new LinkedMultiValueMap<String, Object>();
		values.add("name", name);
		values.add("definition", defintion);

		StreamDefinitionResource stream = restTemplate.postForObject(resources.get("streams"),
				values, StreamDefinitionResource.class);
		return stream;
	}

	@Override
	public void undeployStream(String name) {
		// TODO: discover link by some other means (search by exact name on
		// /streams??)
		String uriTemplate = resources.get("streams").toString() + "/{name}";
		restTemplate
				.delete(uriTemplate, Collections.singletonMap("name", name));
	}
	
	@Override
	public TapDefinitionResource createTap(String name, String definition, Boolean autoStart) {
		String control = ((autoStart != null && autoStart.booleanValue()) ? "start" : "");
		MultiValueMap<String, Object> values = new LinkedMultiValueMap<String, Object>();
		values.add("name", name);
		values.add("definition", definition);
		values.add("control", control);

		TapDefinitionResource tap = restTemplate.postForObject(resources.get("taps"),
				values, TapDefinitionResource.class);
		return tap;
	}

}
