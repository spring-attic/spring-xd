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
 * The REST representation of a Gauge.
 * 
 * @author Luke Taylor
 */
@XmlRootElement(name = "gauge")
public class GaugeResource extends MetricResource {

	// TODO: This is identical to CounterResource. Perhaps we can have an "IntegralResource" instead.
	/**
	 * The value for the gauge.
	 */
	@XmlAttribute(name = "value")
	private long value;

	/**
	 * No-arg constructor for serialization frameworks.
	 */
	protected GaugeResource() {
	}

	public GaugeResource(String name, long value) {
		super(name);
		this.value = value;
	}

	/**
	 * Return the value for the gauge.
	 */
	public long getValue() {
		return value;
	}

}
