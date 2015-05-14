/*
 * Copyright 2002-2015 the original author or authors.
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
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.xd.analytics.metrics.core.Metric;
import org.springframework.xd.analytics.metrics.core.MetricRepository;
import org.springframework.xd.dirt.analytics.NoSuchMetricException;
import org.springframework.xd.rest.domain.metrics.MetricResource;

/**
 * Base class for controllers that expose metrics related resources. Subclasses are meant to provide the root
 * {@link RequestMapping} uri.
 *
 * @author Eric Bottard
 * @author Ilayaperumal Gopinathan
 */
abstract class AbstractMetricsController<R extends MetricRepository<M>, M extends Metric> {

	protected final R repository;

	public AbstractMetricsController(R repository) {
		this.repository = repository;
	}

	protected final ResourceAssembler<M, MetricResource> shallowResourceAssembler = new ShallowMetricResourceAssembler<M>(
			this.getClass());

	/**
	 * Lists metric resources.
	 *
	 * @param pageable
	 * @param pagedAssembler
	 * @param resourceAssembler
	 * @return
	 */
	protected PagedResources<? extends MetricResource> list(Pageable pageable,
			PagedResourcesAssembler<M> pagedAssembler,
			ResourceAssembler<M, ? extends MetricResource> resourceAssembler) {
		/* Page */Iterable<M> metrics = repository.findAll(/* pageable */);

		// Ok for now until we use PagingAndSortingRepo as we know we have lists
		Page<M> page = new PageImpl<M>((List<M>) metrics);
		return pagedAssembler.toResource(page,
				resourceAssembler == null ? shallowResourceAssembler : (ResourceAssembler<M, MetricResource>)resourceAssembler);
	}


	/**
	 * Deletes the metric from the repository
	 *
	 * @param name the name of the metric to delete
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	protected void delete(@PathVariable("name") String name) {
		if (!repository.exists(name)) {
			throw new NoSuchMetricException(name, "Can't delete metric '%s' because it does not exist");
		}
		repository.delete(name);
	}
	// helper code for reset, etc. can go here
}
