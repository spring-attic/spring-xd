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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.PagedResources;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.xd.analytics.metrics.core.Counter;
import org.springframework.xd.analytics.metrics.core.CounterRepository;
import org.springframework.xd.dirt.analytics.NoSuchMetricException;
import org.springframework.xd.rest.domain.metrics.CounterResource;
import org.springframework.xd.rest.domain.metrics.MetricResource;

/**
 * Exposes representations of {@link Counter}s.
 *
 * @author Eric Bottard
 */
@Controller
@RequestMapping("/metrics/counters")
@ExposesResourceFor(CounterResource.class)
public class CountersController extends AbstractMetricsController<CounterRepository, Counter> {

	private final DeepCounterResourceAssembler counterResourceAssembler = new DeepCounterResourceAssembler();

	@Autowired
	public CountersController(CounterRepository counterRepository) {
		super(counterRepository);
	}

	/**
	 * List Counters that match the given criteria.
	 */
	@ResponseBody
	@RequestMapping(value = "", method = RequestMethod.GET)
	public PagedResources<? extends MetricResource> list(Pageable pageable,
			PagedResourcesAssembler<Counter> pagedAssembler,
			@RequestParam(value = "detailed", defaultValue = "false") boolean detailed) {
		return list(pageable, pagedAssembler, detailed ? counterResourceAssembler : shallowResourceAssembler);
	}

	/**
	 * Retrieve information about a specific counter.
	 */
	@ResponseBody
	@RequestMapping(value = "/{name}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public CounterResource display(@PathVariable("name") String name) {
		Counter c = repository.findOne(name);
		if (c == null) {
			throw new NoSuchMetricException(name, "There is no counter named '%s'");
		}
		return counterResourceAssembler.toResource(c);
	}

}
