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
 * A service for managing RichGauge instances.
 *
 * The setValue method updates the current value of the gauge as well as incrementing the internal counter and updating
 * the max, min and average values.
 *
 * It also provides a 'getOrCreate' method that will return an existing gauge given the name or create a new gauge set
 * to zero.
 *
 * Supports a "smoothing constant", "alpha" for use in calculating an <a
 * href="http://www.itl.nist.gov/div898/handbook/pmc/section4/pmc431.htm">exponential moving average</a> for the
 * gauge.
 * The parameter can be changed at any time after the gauge has been created. The mean calculated up to that point will
 * be used to initialize the moving average from that point on.
 *
 * @author Luke Taylor
 *
 */
public interface RichGaugeRepository extends MetricRepository<RichGauge> {

	/**
	 * Set the current value of the gauge, smoothing accumulated values with the provided alpha value.
	 *
	 * @param name the gauge name
	 * @param value the value of the gauge
	 * @param alpha the smoothing factor to use
	 * @throws IllegalArgumentException in case the given name is null
	 */
	void recordValue(String name, double value, double alpha);

	/**
	 * Reset the gauge to zero and reset any accumulated average, max and min values
	 *
	 * @param name the gauge name
	 * @throws IllegalArgumentException in case the given name is null
	 */
	void reset(String name);

}
