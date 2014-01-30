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

import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.xd.rest.client.AggregateCounterOperations;
import org.springframework.xd.rest.client.CodeCompletionOperations;
import org.springframework.xd.rest.client.CounterOperations;
import org.springframework.xd.rest.client.FieldValueCounterOperations;
import org.springframework.xd.rest.client.GaugeOperations;
import org.springframework.xd.rest.client.JobOperations;
import org.springframework.xd.rest.client.ModuleOperations;
import org.springframework.xd.rest.client.RichGaugeOperations;
import org.springframework.xd.rest.client.RuntimeOperations;
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
	 * Holds the Module definition related part of the API.
	 */
	private ModuleOperations moduleOperations;

	/**
	 * Holds the Module-related part of the API.
	 */
	private RuntimeOperations runtimeOperations;

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

	/**
	 * Holds the code completion related part of the API.
	 */
	private CodeCompletionOperations codeCompletionOperations;

	public SpringXDTemplate(ClientHttpRequestFactory factory, URI baseURI) {
		super(factory);
		XDRuntime xdRuntime = restTemplate.getForObject(baseURI, XDRuntime.class);
		resources.put("streams", URI.create(xdRuntime.getLink("streams").getHref()));
		resources.put("jobs", URI.create(xdRuntime.getLink("jobs").getHref()));
		resources.put("batch/jobs", URI.create(xdRuntime.getLink("batch/jobs").getHref()));
		resources.put("batch/executions", URI.create(xdRuntime.getLink("batch/executions").getHref()));
		resources.put("runtime/containers", URI.create(xdRuntime.getLink("runtime/containers").getHref()));
		resources.put("runtime/modules", URI.create(xdRuntime.getLink("runtime/modules").getHref()));
		resources.put("modules", URI.create(xdRuntime.getLink("modules").getHref()));
		resources.put("completions/stream", URI.create(xdRuntime.getLink("completions/stream").getHref()));
		resources.put("completions/job", URI.create(xdRuntime.getLink("completions/job").getHref()));
		resources.put("completions/composed", URI.create(xdRuntime.getLink("completions/composed").getHref()));

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
		moduleOperations = new ModuleTemplate(this);
		runtimeOperations = new RuntimeTemplate(this);
		codeCompletionOperations = new CodeCompletionTemplate(this);
	}

	public SpringXDTemplate(URI baseURI) {
		this(new SimpleClientHttpRequestFactory(), baseURI);
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
	public ModuleOperations moduleOperations() {
		return moduleOperations;
	}

	@Override
	public RuntimeOperations runtimeOperations() {
		return runtimeOperations;
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

	@Override
	public CodeCompletionOperations codeCompletionOperations() {
		return codeCompletionOperations;
	}
}
