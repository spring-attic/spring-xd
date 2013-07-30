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

import org.joda.time.DateTime;
import org.joda.time.DateTimeField;
import org.joda.time.Interval;
import org.joda.time.ReadablePeriod;
import org.joda.time.chrono.ISOChronology;
import org.joda.time.format.ISOPeriodFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.xd.analytics.metrics.core.AggregateCount;
import org.springframework.xd.analytics.metrics.core.AggregateCounterRepository;
import org.springframework.xd.analytics.metrics.core.Counter;
import org.springframework.xd.analytics.metrics.memory.InMemoryAggregateCounter;
import org.springframework.xd.rest.client.domain.metrics.AggregateCountsResource;

/**
 * Exposes representations of {@link InMemoryAggregateCounter}s.
 * 
 * @author Eric Bottard
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
	 * Retrieve counts for a given time interval, using some precision.
	 * 
	 * @param name the name of the aggregate counter we want to retrieve data from
	 * @param from the start-time for the interval, default depends on the resolution (e.g. go back 1 day for hourly
	 * buckets)
	 * @param to the end-time for the interval, default "now"
	 * @param resolution the size of buckets to aggregate, <i>e.g.</i> hourly, daily, <i>etc.</i> (default "hour")
	 */
	@ResponseBody
	@RequestMapping(value = "/{name}", method = RequestMethod.GET)
	public AggregateCountsResource display(@PathVariable("name")
	String name,//
			@RequestParam(value = "from", required = false)
			@DateTimeFormat(iso = ISO.DATE_TIME)
			DateTime from,//
			@RequestParam(value = "to", required = false)
			@DateTimeFormat(iso = ISO.DATE_TIME)
			DateTime to, //
			@RequestParam(value = "resolution", defaultValue = "hour")
			Resolution resolution) {

		if (to == null) {
			to = new DateTime();
		}
		if (from == null) {
			from = to.minus(resolution.rewindDuration);
		}

		AggregateCount aggregate = repository.getCounts(name, new Interval(from, to), resolution.toJoda());

		return aggregateCountResourceAssembler.toResource(aggregate);
	}

	/**
	 * The resolution at which to collect data between two dates.
	 * 
	 * @author Eric Bottard
	 */
	public static enum Resolution {

		/**
		 * One point per minute.
		 */
		minute(ISOChronology.getInstanceUTC().minuteOfHour(), ISOPeriodFormat.standard().parsePeriod("PT1H")),

		/**
		 * One point per hour.
		 */
		hour(ISOChronology.getInstanceUTC().hourOfDay(), ISOPeriodFormat.standard().parsePeriod("P1D"));

		private final DateTimeField joda;

		/**
		 * A human friendly duration that makes sense given this resolution.
		 */
		private final ReadablePeriod rewindDuration;

		private Resolution(DateTimeField joda, ReadablePeriod rewindDuration) {
			this.joda = joda;
			this.rewindDuration = rewindDuration;
		}

		DateTimeField toJoda() {
			return joda;
		}
	}

}
