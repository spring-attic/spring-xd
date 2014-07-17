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

package org.springframework.xd.rest.client;

import java.util.Date;

import org.springframework.hateoas.PagedResources;
import org.springframework.xd.rest.domain.metrics.AggregateCountsResource;
import org.springframework.xd.rest.domain.metrics.MetricResource;

/**
 * Interface defining operations available when dealing with Aggregate Counters.
 * 
 * @author Ilayaperumal Gopinathan
 */
public interface AggregateCounterOperations {

	/**
	 * Retrieve the information for the given named AggregateCounter
	 * 
	 * @param name the name of the aggregate counter to retrieve information for
	 */
	AggregateCountsResource retrieve(String name, Date from, Date to, Resolution resolution);

	/**
	 * List the names of the available aggregate counters
	 */
	PagedResources<MetricResource> list();

	/**
	 * Delete the given named aggregate counter
	 * 
	 * @param name the name of the aggregate counter to delete
	 */
	void delete(String name);

	public static enum Resolution {
		minute, hour, day, month;

	};

}
