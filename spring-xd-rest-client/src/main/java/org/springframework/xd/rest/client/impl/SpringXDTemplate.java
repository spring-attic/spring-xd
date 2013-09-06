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

package org.springframework.xd.rest.client.impl;

import java.net.URI;

import org.springframework.xd.rest.client.AggregateCounterOperations;
import org.springframework.xd.rest.client.CounterOperations;
import org.springframework.xd.rest.client.FieldValueCounterOperations;
import org.springframework.xd.rest.client.GaugeOperations;
import org.springframework.xd.rest.client.JobOperations;
import org.springframework.xd.rest.client.RichGaugeOperations;
import org.springframework.xd.rest.client.SpringXDOperations;
import org.springframework.xd.rest.client.StreamOperations;
import org.springframework.xd.rest.client.domain.XDRuntime;

/**
 * Implementation of the entry point to the API.
 * 
 * @author Eric Bottard
 */
public class SpringXDTemplate extends AbstractTemplate implements SpringXDOperations {

	/**
	 * Holds the Stream-related part of the API.
	 */
	private StreamOperations streamOperations;

	/**
	 * Holds the Job-related part of the API.
	 */
	private JobOperations jobOperations;

	/**
	 * Holds the Counter-related part of the API.
	 */
	private CounterOperations counterOperations;

	/**
	 * Holds the Field Value Counter related part of the API.
	 */
	private FieldValueCounterOperations fvcOperations;

	/**
	 * Holds the Aggregate counter related part of the API
	 */
	private AggregateCounterOperations aggrCounterOperations;

	/**
	 * Holds the Gauge related part of the API
	 */
	private GaugeOperations gaugeOperations;

	/**
	 * Holds the RichGauge related part of the API
	 */
	private RichGaugeOperations richGaugeOperations;

	public SpringXDTemplate(URI baseURI) {
		XDRuntime xdRuntime = restTemplate.getForObject(baseURI, XDRuntime.class);
		resources.put("streams", URI.create(xdRuntime.getLink("streams").getHref()));
		resources.put("jobs", URI.create(xdRuntime.getLink("jobs").getHref()));

		resources.put("counters", URI.create(xdRuntime.getLink("counters").getHref()));
		resources.put("field-value-counters", URI.create(xdRuntime.getLink("field-value-counters").getHref()));
		resources.put("aggregate-counters", URI.create(xdRuntime.getLink("aggregate-counters").getHref()));
		resources.put("gauges", URI.create(xdRuntime.getLink("gauges").getHref()));
		resources.put("richgauges", URI.create(xdRuntime.getLink("richgauges").getHref()));

		streamOperations = new StreamTemplate(this);
		jobOperations = new JobTemplate(this);
		counterOperations = new CounterTemplate(this);
		fvcOperations = new FieldValueCounterTemplate(this);
		aggrCounterOperations = new AggregateCounterTemplate(this);
		gaugeOperations = new GaugeTemplate(this);
		richGaugeOperations = new RichGaugeTemplate(this);
	}

	@Override
	public StreamOperations streamOperations() {
		return streamOperations;
	}

	@Override
	public JobOperations jobOperations() {
		return jobOperations;
	}

	@Override
	public CounterOperations counterOperations() {
		return counterOperations;
	}

	@Override
	public FieldValueCounterOperations fvcOperations() {
		return fvcOperations;
	}

	@Override
	public AggregateCounterOperations aggrCounterOperations() {
		return aggrCounterOperations;
	}

	@Override
	public GaugeOperations gaugeOperations() {
		return gaugeOperations;
	}

	@Override
	public RichGaugeOperations richGaugeOperations() {
		return richGaugeOperations;
	}
}
