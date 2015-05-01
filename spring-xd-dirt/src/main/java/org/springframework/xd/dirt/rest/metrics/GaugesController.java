/*
 * Copyright 2013-2015 the original author or authors.
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

package org.springframework.xd.dirt.rest.metrics;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.PagedResources;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.xd.analytics.metrics.core.Gauge;
import org.springframework.xd.analytics.metrics.core.GaugeRepository;
import org.springframework.xd.dirt.analytics.NoSuchMetricException;
import org.springframework.xd.rest.domain.metrics.GaugeResource;
import org.springframework.xd.rest.domain.metrics.MetricResource;

/**
 * Exposes representations of {@link Gauge}s.
 *
 * @author Luke Taylor
 */
@Controller
@RequestMapping("/metrics/gauges")
@ExposesResourceFor(GaugeResource.class)
public class GaugesController extends AbstractMetricsController<GaugeRepository, Gauge> {

	private final DeepGaugeResourceAssembler gaugeResourceAssembler = new DeepGaugeResourceAssembler();

	@Autowired
	public GaugesController(GaugeRepository repository) {
		super(repository);
	}

	@ResponseBody
	@RequestMapping(value = "", method = RequestMethod.GET)
	public PagedResources<? extends MetricResource> list(Pageable pageable,
			PagedResourcesAssembler<Gauge> pagedAssembler,
			@RequestParam(value = "detailed", defaultValue = "false") boolean detailed) {
		return list(pageable, pagedAssembler, detailed ? gaugeResourceAssembler : shallowResourceAssembler);
	}

	@ResponseBody
	@RequestMapping(value = "/{name}", method = RequestMethod.GET)
	public GaugeResource display(@PathVariable("name") String name) {
		Gauge g = repository.findOne(name);
		if (g == null) {
			throw new NoSuchMetricException(name, "There is no gauge named '%s'");
		}
		return gaugeResourceAssembler.toResource(g);
	}

}
