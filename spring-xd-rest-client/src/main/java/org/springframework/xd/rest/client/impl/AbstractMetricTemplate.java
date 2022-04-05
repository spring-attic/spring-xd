/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.xd.rest.client.impl;

import org.springframework.hateoas.PagedResources;
import org.springframework.xd.rest.domain.metrics.MetricResource;


/**
 *
 * @author Paul Harris
 * @since 1.2
 */
abstract class AbstractMetricTemplate extends AbstractTemplate {

	private final String resourcesKey;

	public AbstractMetricTemplate(AbstractTemplate other, String resourcesKey) {
		super(other);
		this.resourcesKey = resourcesKey;
	}

	public PagedResources<MetricResource> list() {
		String url = resources.get(this.resourcesKey).toString() + "?size=10000";
		return restTemplate.getForObject(url, MetricResource.Page.class);
	}

	public void delete(String name) {
		String url = resources.get(this.resourcesKey).toString() + "/{name}";
		restTemplate.delete(url, name);
	}

}
