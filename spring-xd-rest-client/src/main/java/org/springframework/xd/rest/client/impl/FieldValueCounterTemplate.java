/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.rest.client.impl;

import org.springframework.hateoas.PagedResources;
import org.springframework.xd.rest.client.FieldValueCounterOperations;
import org.springframework.xd.rest.domain.metrics.FieldValueCounterResource;
import org.springframework.xd.rest.domain.metrics.MetricResource;

/**
 * Implementation of the Field Value Counter part of the metrics API.
 * 
 * @author Eric Bottard
 */
public class FieldValueCounterTemplate extends AbstractTemplate implements FieldValueCounterOperations {

	public FieldValueCounterTemplate(AbstractTemplate abstractTemplate) {
		super(abstractTemplate);
	}

	@Override
	public FieldValueCounterResource retrieve(String name) {
		String url = resources.get("field-value-counters").toString() + "/{name}";
		return restTemplate.getForObject(url, FieldValueCounterResource.class, name);
	}

	@Override
	public PagedResources<MetricResource> list() {
		String url = resources.get("field-value-counters").toString() + "?page=10000";
		return restTemplate.getForObject(url, MetricResource.Page.class);
	}

	@Override
	public void delete(String name) {
		String url = resources.get("field-value-counters").toString() + "/{name}";
		restTemplate.delete(url, name);
	}

}
