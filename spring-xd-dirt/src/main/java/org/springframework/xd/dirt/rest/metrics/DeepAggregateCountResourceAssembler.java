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

import java.util.Date;

import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import org.springframework.xd.analytics.metrics.core.AggregateCount;
import org.springframework.xd.rest.client.domain.metrics.AggregateCountsResource;

/**
 * Knows how to construct {@link AggregateCountsResource} out of {@link AggregateCount}.
 * 
 * @author Eric Bottard
 */
public class DeepAggregateCountResourceAssembler extends
		ResourceAssemblerSupport<AggregateCount, AggregateCountsResource> {

	public DeepAggregateCountResourceAssembler() {
		super(AggregateCountersController.class, AggregateCountsResource.class);
	}

	@Override
	public AggregateCountsResource toResource(AggregateCount entity) {
		return createResourceWithId(entity.name, entity);
	}

	@Override
	protected AggregateCountsResource instantiateResource(AggregateCount entity) {
		AggregateCountsResource result = new AggregateCountsResource(entity.name);
		long increment = entity.resolution.getDurationField().getUnitMillis();
		long end = entity.interval.getEndMillis();
		int i = 0;
		for (long when = entity.interval.getStartMillis(); when <= end; when += increment) {
			result.addValue(new Date(when), entity.counts[i++]);
		}
		return result;
	}
}
