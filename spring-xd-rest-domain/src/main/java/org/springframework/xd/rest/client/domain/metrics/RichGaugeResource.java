/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.xd.rest.client.domain.metrics;

/**
 * The REST representation of a RichGauge.
 *
 * @author Luke Taylor
 */
public class RichGaugeResource extends MetricResource {
	/**
	 * The value for the gauge.
	 */
	private double value;
	private double average;
	private double max;
	private double min;

	/**
	 * No-arg constructor for serialization frameworks.
	 */
	protected RichGaugeResource() {
	}

	public RichGaugeResource(String name, double value, double average, double max, double min) {
		super(name);
		this.value = value;
		this.average = average;
		this.max = max;
		this.min = min;
	}

	/**
	 * Return the value for the gauge.
	 */
	public double getValue() {
		return value;
	}

	/**
	 * Return the average value for the gauge. This will be the arithmetic
	 * mean unless an 'alpha' value has been set for the gauge.
	 */
	public double getAverage() {
		return average;
	}

	public double getMax() {
		return max;
	}

	public double getMin() {
		return min;
	}
}
