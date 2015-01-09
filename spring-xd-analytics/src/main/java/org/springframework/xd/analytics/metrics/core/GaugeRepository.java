/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.xd.analytics.metrics.core;

/**
 * A marker interface for Gauge repositories.
 * 
 * @author Mark Pollack
 * 
 */
public interface GaugeRepository extends MetricRepository<Gauge> {

	/**
	 * Set the value of the gauge.
	 * 
	 * @param name the gauge name
	 * @param value the value of the gauge
	 * @throws IllegalArgumentException in case the given name is null
	 */
	void recordValue(String name, long value);

	/**
	 * Reset the gauge to zero
	 * 
	 * @param name the gauge name
	 * @throws IllegalArgumentException in case the given name is null
	 */
	void reset(String name);
}
