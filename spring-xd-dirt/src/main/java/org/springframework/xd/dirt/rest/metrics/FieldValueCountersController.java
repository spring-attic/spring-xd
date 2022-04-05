/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
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
import org.springframework.xd.analytics.metrics.core.FieldValueCounter;
import org.springframework.xd.analytics.metrics.core.FieldValueCounterRepository;
import org.springframework.xd.dirt.analytics.NoSuchMetricException;
import org.springframework.xd.rest.domain.metrics.FieldValueCounterResource;
import org.springframework.xd.rest.domain.metrics.MetricResource;

/**
 * Controller that exposes {@link FieldValueCounter} related representations.
 *
 * @author Eric Bottard
 */
@Controller
@RequestMapping("/metrics/field-value-counters")
@ExposesResourceFor(FieldValueCounterResource.class)
public class FieldValueCountersController extends
		AbstractMetricsController<FieldValueCounterRepository, FieldValueCounter> {

	/**
	 * Used to create rich representations of a {@link FieldValueCounter}.
	 */
	private final DeepFieldValueCounterResourceAssembler fvcResourceAssembler = new DeepFieldValueCounterResourceAssembler();

	@Autowired
	public FieldValueCountersController(FieldValueCounterRepository repository) {
		super(repository);
	}

	/**
	 * List {@link FieldValueCounter}s that match the given criteria.
	 */
	@ResponseBody
	@RequestMapping(value = "", method = RequestMethod.GET)
	public PagedResources<? extends MetricResource> list(Pageable pageable,
			PagedResourcesAssembler<FieldValueCounter> pagedAssembler,
			@RequestParam(value = "detailed", defaultValue = "false") boolean detailed) {
		return list(pageable, pagedAssembler, detailed ? fvcResourceAssembler : shallowResourceAssembler);
	}

	@ResponseBody
	@RequestMapping(value = "/{name}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public FieldValueCounterResource display(@PathVariable("name") String name) {
		FieldValueCounter c = repository.findOne(name);
		if (c == null) {
			throw new NoSuchMetricException(name, "There is no field-value-counter named '%s'");
		}
		return fvcResourceAssembler.toResource(c);
	}

}
