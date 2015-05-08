/*
 * Copyright 2013-2015 the original author or authors.
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

import org.springframework.hateoas.UriTemplate;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.xd.rest.client.AggregateCounterOperations;
import org.springframework.xd.rest.client.CompletionOperations;
import org.springframework.xd.rest.client.CounterOperations;
import org.springframework.xd.rest.client.FieldValueCounterOperations;
import org.springframework.xd.rest.client.GaugeOperations;
import org.springframework.xd.rest.client.JobOperations;
import org.springframework.xd.rest.client.ModuleOperations;
import org.springframework.xd.rest.client.RichGaugeOperations;
import org.springframework.xd.rest.client.RuntimeOperations;
import org.springframework.xd.rest.client.SpringXDOperations;
import org.springframework.xd.rest.client.StreamOperations;
import org.springframework.xd.rest.domain.XDRuntime;

/**
 * Implementation of the entry point to the API.
 *
 * @author Eric Bottard
 * @author David Turanski
 * @author Paul Harris
 * @author Gary Russell
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
	private CompletionOperations completionOperations;

	public SpringXDTemplate(URI baseURI) {
		this(baseURI, null, null, null, null, null);
	}

	public SpringXDTemplate(ClientHttpRequestFactory factory, URI baseURI) {
		this(factory, baseURI, null, null, null, null, null);
	}

	public SpringXDTemplate(URI baseURI, String rabbitMqAdminUri,
			String rabbitMqPassword, String rabbitMqUsername, String rabbitMqVhost, String busPrefix) {
		this(new SimpleClientHttpRequestFactory(), baseURI, rabbitMqAdminUri, rabbitMqPassword, rabbitMqUsername,
				rabbitMqVhost, busPrefix);
	}

	public SpringXDTemplate(ClientHttpRequestFactory factory, URI baseURI, String rabbitMqAdminUri,
			String rabbitMqPassword, String rabbitMqUsername, String rabbitMqVhost, String busPrefix) {
		super(factory);
		XDRuntime xdRuntime = restTemplate.getForObject(baseURI, XDRuntime.class);

		resources.put("streams/definitions", new UriTemplate(xdRuntime.getLink("streams").getHref() + "/definitions"));
		resources.put("streams/deployments", new UriTemplate(xdRuntime.getLink("streams").getHref() + "/deployments"));
		resources.put("jobs", new UriTemplate(xdRuntime.getLink("jobs").getHref()));
		resources.put("jobs/definitions", new UriTemplate(xdRuntime.getLink("jobs").getHref() + "/definitions"));
		resources.put("jobs/deployments", new UriTemplate(xdRuntime.getLink("jobs").getHref() + "/deployments"));
		resources.put("modules", new UriTemplate(xdRuntime.getLink("modules").getHref()));

		resources.put("jobs/configurations", new UriTemplate(xdRuntime.getLink("jobs/configurations").getHref()));
		resources.put("jobs/executions", new UriTemplate(xdRuntime.getLink("jobs/executions").getHref()));
		resources.put("jobs/instances", new UriTemplate(xdRuntime.getLink("jobs/instances").getHref()));

		resources.put("runtime/containers", new UriTemplate(xdRuntime.getLink("runtime/containers").getHref()));
		resources.put("runtime/modules", new UriTemplate(xdRuntime.getLink("runtime/modules").getHref()));

		resources.put("completions/stream", new UriTemplate(xdRuntime.getLink("completions/stream").getHref()));
		resources.put("completions/job", new UriTemplate(xdRuntime.getLink("completions/job").getHref()));
		resources.put("completions/module", new UriTemplate(xdRuntime.getLink("completions/module").getHref()));

		resources.put("counters", new UriTemplate(xdRuntime.getLink("counters").getHref()));
		resources.put("field-value-counters", new UriTemplate(xdRuntime.getLink("field-value-counters").getHref()));
		resources.put("aggregate-counters", new UriTemplate(xdRuntime.getLink("aggregate-counters").getHref()));
		resources.put("gauges", new UriTemplate(xdRuntime.getLink("gauges").getHref()));
		resources.put("rich-gauges", new UriTemplate(xdRuntime.getLink("rich-gauges").getHref()));

		resources.put("jobs/clean/rabbit", new UriTemplate(xdRuntime.getLink("jobs").getHref() + "/clean/rabbit"));
		resources.put("streams/clean/rabbit", new UriTemplate(xdRuntime.getLink("streams").getHref() + "/clean/rabbit"));

		streamOperations = new StreamTemplate(this, rabbitMqAdminUri, rabbitMqPassword, rabbitMqUsername,
				rabbitMqVhost, busPrefix);
		jobOperations = new JobTemplate(this, rabbitMqAdminUri, rabbitMqPassword, rabbitMqUsername, rabbitMqVhost,
				busPrefix);
		counterOperations = new CounterTemplate(this);
		fvcOperations = new FieldValueCounterTemplate(this);
		aggrCounterOperations = new AggregateCounterTemplate(this);
		gaugeOperations = new GaugeTemplate(this);
		richGaugeOperations = new RichGaugeTemplate(this);
		moduleOperations = new ModuleTemplate(this);
		runtimeOperations = new RuntimeTemplate(this);
		completionOperations = new CompletionTemplate(this);
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
	public CompletionOperations completionOperations() {
		return completionOperations;
	}
}
