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

package org.springframework.xd.dirt.rest.metrics;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.xd.analytics.metrics.core.Metric;
import org.springframework.xd.analytics.metrics.core.MetricRepository;
import org.springframework.xd.rest.client.domain.metrics.MetricResource;

/**
 * Base class for controllers that expose metrics related resources. Subclasses are meant
 * to provide the root {@link RequestMapping} uri.
 * 
 * @author Eric Bottard
 */
public abstract class AbstractMetricsController<R extends MetricRepository<M>, M extends Metric> {

	protected R repository;

	public AbstractMetricsController(R repository) {
		this.repository = repository;
	}

	private final ResourceAssembler<M, MetricResource> shallowResourceAssembler = new ShallowMetricResourceAssembler<M>(
			this.getClass());

	protected PagedResources<MetricResource> list(Pageable pageable,
			PagedResourcesAssembler<M> pagedAssembler) {
		/* Page */Iterable<M> metrics = repository.findAll(/* pageable */);
		Page<M> page = new PageImpl((List<M>) metrics);
		return pagedAssembler.toResource(page, shallowResourceAssembler);
	}

}
