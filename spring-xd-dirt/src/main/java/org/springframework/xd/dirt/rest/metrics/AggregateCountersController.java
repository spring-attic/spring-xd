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

import java.util.LinkedList;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.Interval;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.PagedResources;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.xd.analytics.metrics.core.AggregateCount;
import org.springframework.xd.analytics.metrics.core.AggregateCountResolution;
import org.springframework.xd.analytics.metrics.core.AggregateCounterRepository;
import org.springframework.xd.analytics.metrics.core.Counter;
import org.springframework.xd.rest.domain.metrics.AggregateCountsResource;
import org.springframework.xd.rest.domain.metrics.MetricResource;

/**
 * Exposes representations of {@link AggregateCount}s.
 *
 * @author Eric Bottard
 * @author Ilayaperumal Gopinathan
 */
@Controller
@RequestMapping("/metrics/aggregate-counters")
@ExposesResourceFor(AggregateCountsResource.class)
public class AggregateCountersController extends AbstractMetricsController<AggregateCounterRepository, Counter> {

	private final DeepAggregateCountResourceAssembler aggregateCountResourceAssembler = new DeepAggregateCountResourceAssembler();

	@Autowired
	public AggregateCountersController(AggregateCounterRepository repository) {
		super(repository);
	}

	/**
	 * List {@link AggregateCount}s that match the given criteria.
	 */
	@ResponseBody
	@RequestMapping(value = "", method = RequestMethod.GET)
	public PagedResources<? extends MetricResource> list(Pageable pageable,
			PagedResourcesAssembler<Counter> pagedAssembler,
			@RequestParam(value = "detailed", defaultValue = "false") boolean detailed,//
			@RequestParam(value = "from", required = false) @DateTimeFormat(iso = ISO.DATE_TIME) DateTime from, //
			@RequestParam(value = "to", required = false) @DateTimeFormat(iso = ISO.DATE_TIME) DateTime to, //
			@RequestParam(value = "resolution", defaultValue = "hour") AggregateCountResolution resolution) {
		PagedResources<? extends MetricResource> resources = list(pageable, pagedAssembler, shallowResourceAssembler);
		if (detailed) {
			to = providedOrDefaultToValue(to);
			from = providedOrDefaultFromValue(from, to, resolution);
			Interval interval = new Interval(from, to);

			List<AggregateCountsResource> aggregateCounts = new LinkedList<>();
			for (MetricResource metricResource : resources) {
				AggregateCount aggregateCount = repository.getCounts(metricResource.getName(), interval, resolution);
				aggregateCounts.add(aggregateCountResourceAssembler.toResource(aggregateCount));
			}
			return new PagedResources<>(aggregateCounts, resources.getMetadata());
		}
		return resources;
	}

	/**
	 * Retrieve counts for a given time interval, using some precision.
	 *
	 * @param name the name of the aggregate counter we want to retrieve data from
	 * @param from the start-time for the interval, default depends on the resolution (e.g. go back 1 day for hourly
	 *        buckets)
	 * @param to the end-time for the interval, default "now"
	 * @param resolution the size of buckets to aggregate, <i>e.g.</i> hourly, daily, <i>etc.</i> (default "hour")
	 */
	@ResponseBody
	@RequestMapping(value = "/{name}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public AggregateCountsResource display(@PathVariable("name") String name, //
			@RequestParam(value = "from", required = false) @DateTimeFormat(iso = ISO.DATE_TIME) DateTime from, //
			@RequestParam(value = "to", required = false) @DateTimeFormat(iso = ISO.DATE_TIME) DateTime to, //
			@RequestParam(value = "resolution", defaultValue = "hour") AggregateCountResolution resolution) {

		to = providedOrDefaultToValue(to);
		from = providedOrDefaultFromValue(from, to, resolution);

		AggregateCount aggregate = repository.getCounts(name, new Interval(from, to), resolution);

		return aggregateCountResourceAssembler.toResource(aggregate);
	}

	/**
	 * Return a default value for the interval end if none has been provided.
	 */
	private DateTime providedOrDefaultToValue(DateTime to) {
		if (to == null) {
			to = new DateTime();
		}
		return to;
	}

	/**
	 * Return a default value for the interval start if none has been provided.
	 */
	private DateTime providedOrDefaultFromValue(DateTime from, DateTime to, AggregateCountResolution resolution) {
		if (from != null) {
			return from;
		}
		switch (resolution) {
			case minute:
				return to.minusMinutes(59);
			case hour:
				return to.minusHours(23);
			case day:
				return to.minusDays(6);
			case month:
				return to.minusMonths(11);
			case year:
				return to.minusYears(4);
			default:
				throw new IllegalStateException("Shouldn't happen. Unhandled resolution: " + resolution);
		}
	}

}
