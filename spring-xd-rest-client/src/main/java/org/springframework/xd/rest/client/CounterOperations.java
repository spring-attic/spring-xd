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

import org.springframework.hateoas.PagedResources;
import org.springframework.xd.rest.domain.metrics.CounterResource;
import org.springframework.xd.rest.domain.metrics.MetricResource;

/**
 * Interface defining operations available when dealing with Counters.
 * 
 * @author Eric Bottard
 */
public interface CounterOperations {

	/**
	 * Retrieve information about the given named counter.
	 */
	CounterResource retrieve(String name);

	/**
	 * Retrieve basic information (i.e. names) for existing counters.
	 */
	PagedResources<MetricResource> list(/* TODO */);

	/**
	 * Delete the counter with given name
	 */
	void delete(String name);
}
