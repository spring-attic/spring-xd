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

import org.springframework.xd.rest.domain.metrics.MetricResource;

/**
 *
 * @param <T> the metric type.
 * @author Paul Harris
 * @since 1.2
 */
abstract class AbstractSingleMetricTemplate<T extends MetricResource> extends AbstractMetricTemplate {

	private final String resourcesKey;

	private final Class<T> resourceType;

	public AbstractSingleMetricTemplate(AbstractTemplate other, String resourcesKey, Class<T> resourceType) {
		super(other, resourcesKey);
		this.resourcesKey = resourcesKey;
		this.resourceType = resourceType;
	}

	public T retrieve(String name) {
		String url = resources.get(this.resourcesKey).toString() + "/{name}";
		return restTemplate.getForObject(url, this.resourceType, name);
	}
}
