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

package org.springframework.xd.rest.domain.metrics;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * The REST representation of a RichGauge.
 * 
 * @author Luke Taylor
 * @author Ilayaperumal Gopinathan
 */
@XmlRootElement(name = "rich-gauge")
public class RichGaugeResource extends MetricResource {

	/**
	 * The value for the gauge.
	 */
	@XmlAttribute(name = "value")
	private double value;

	@XmlAttribute(name = "alpha")
	private double alpha;

	@XmlAttribute(name = "average")
	private double average;

	@XmlAttribute(name = "max")
	private double max;

	@XmlAttribute(name = "min")
	private double min;

	@XmlAttribute(name = "count")
	private long count;

	/**
	 * No-arg constructor for serialization frameworks.
	 */
	protected RichGaugeResource() {
	}

	public RichGaugeResource(String name, double value, double alpha, double average, double max, double min, long count) {
		super(name);
		this.value = value;
		this.alpha = alpha;
		this.average = average;
		this.max = max;
		this.min = min;
		this.count = count;
	}

	/**
	 * Return the value for the gauge.
	 */
	public double getValue() {
		return value;
	}

	public double getAlpha() {
		return alpha;
	}

	/**
	 * Return the average value for the gauge. This will be the arithmetic mean unless an 'alpha' value has been set for
	 * the gauge.
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

	public long getCount() {
		return count;
	}
}
