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

package org.springframework.xd.rest.client.impl;

import java.util.Date;

import org.joda.time.DateTime;

import org.springframework.util.Assert;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.xd.rest.client.AggregateCounterOperations;
import org.springframework.xd.rest.domain.metrics.AggregateCountsResource;

/**
 * Implementation of the Aggregate Counter part of the metrics API.
 *
 * @author Ilayaperumal Gopinathan
 * @author Paul Harris
 */
public class AggregateCounterTemplate extends AbstractMetricTemplate implements AggregateCounterOperations {

	public AggregateCounterTemplate(AbstractTemplate abstractTemplate) {
		super(abstractTemplate, "aggregate-counters");
	}

	@Override
	public AggregateCountsResource retrieve(String name, Date from, Date to, Resolution resolution) {
		Assert.notNull(resolution, "Resolution must not be null");
		DateTime fromParam = (from == null) ? null : new DateTime(from.getTime());
		DateTime toParam = (to == null) ? null : new DateTime(to.getTime());

		String url = resources.get("aggregate-counters").toString() + "/{name}";
		String uriString = UriComponentsBuilder.fromUriString(url).queryParam("resolution", resolution.toString())
				.queryParam("from", fromParam).queryParam("to", toParam).build().toUriString();
		return restTemplate.getForObject(uriString, AggregateCountsResource.class, name);
	}

}
