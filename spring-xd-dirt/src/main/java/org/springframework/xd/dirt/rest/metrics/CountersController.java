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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.PagedResources;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.xd.analytics.metrics.core.Counter;
import org.springframework.xd.analytics.metrics.core.CounterRepository;
import org.springframework.xd.rest.client.domain.metrics.CounterResource;
import org.springframework.xd.rest.client.domain.metrics.MetricResource;

/**
 * Exposes representations of {@link Counter}s.
 * 
 * @author Eric Bottard
 */
@Controller
@RequestMapping("/metrics/counters")
@ExposesResourceFor(CounterResource.class)
public class CountersController extends
		AbstractMetricsController<CounterRepository, Counter> {

	@Autowired
	public CountersController(CounterRepository repository) {
		super(repository);
	}

	@Override
	@ResponseBody
	@RequestMapping(value = "", method = RequestMethod.GET)
	public PagedResources<MetricResource> list(Pageable pageable,
			PagedResourcesAssembler<Counter> pagedAssembler) {
		return super.list(pageable, pagedAssembler);
	}
}
