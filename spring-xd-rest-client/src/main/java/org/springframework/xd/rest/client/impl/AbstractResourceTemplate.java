/*
 * Copyright 2015 the original author or authors.
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
import java.util.Collections;
import java.util.Map;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.xd.rest.client.ResourceOperations;
import org.springframework.xd.rest.domain.support.DeploymentPropertiesFormat;

/**
 * @author Paul Harris
 * @author Gary Russell
 * @since 1.2
 */
abstract class AbstractResourceTemplate extends AbstractTemplate implements ResourceOperations {

	private final String adminUri;

	private final String password;

	private final String resourceType;

	private final String username;

	private final String vhost;

	private final String busPrefix;

	public AbstractResourceTemplate(AbstractTemplate other, String adminUri, String password, String resourceType,
			String username, String vhost, String busPrefix) {
		super(other);
		this.adminUri = adminUri;
		this.password = password;
		this.resourceType = resourceType;
		this.username = username;
		this.vhost = vhost;
		this.busPrefix = busPrefix;
	}

	@Override
	public void destroy(String name) {
		// TODO: discover link by some other means (search by exact name on
		// resources??)
		String uriTemplate = resources.get(this.resourceType + "/definitions").toString() + "/{name}";
		restTemplate.delete(uriTemplate, Collections.singletonMap("name", name));
	}

	@Override
	public void deploy(String name, Map<String, String> properties) {
		// TODO: discover link by some other means (search by exact name on
		// resources??)
		String uriTemplate = resources.get(this.resourceType + "/deployments").toString() + "/{name}";
		MultiValueMap<String, Object> values = new LinkedMultiValueMap<String, Object>();
		values.add("properties", DeploymentPropertiesFormat.formatDeploymentProperties(properties));
		//TODO: Do we need ResourceDeploymentResource?
		restTemplate.postForObject(uriTemplate, values, Object.class, name);
	}

	@Override
	public void undeploy(String name) {
		// TODO: discover link by some other means (search by exact name on
		// /resources??)
		String uriTemplate = resources.get(this.resourceType + "/deployments").toString() + "/{name}";
		restTemplate.delete(uriTemplate, name);
	}

	@Override
	public void undeployAll() {
		restTemplate.delete(resources.get(this.resourceType + "/deployments").expand());
	}

	@Override
	public void destroyAll() {
		restTemplate.delete(resources.get(this.resourceType + "/definitions").expand());
	}

	@Override
	public void cleanBusResources(String name) {
		String rootUri = resources.get(this.resourceType + "/clean/rabbit").toString() + "/{name}";
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(rootUri);

		if (this.adminUri != null) {
			builder.queryParam("adminUri", this.adminUri);
		}

		if (this.password != null) {
			builder.queryParam("pw", this.password);
		}

		if (this.username != null) {
			builder.queryParam("user", this.username);
		}

		if (this.vhost != null) {
			builder.queryParam("vhost", this.vhost);
		}

		if (this.busPrefix != null) {
			builder.queryParam("busPrefix", this.busPrefix);
		}

		URI uri = builder.buildAndExpand(Collections.singletonMap("name", name)).toUri();
		restTemplate.delete(uri);
	}

}
