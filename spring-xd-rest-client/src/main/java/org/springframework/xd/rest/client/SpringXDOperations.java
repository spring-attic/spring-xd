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

package org.springframework.xd.rest.client;

/**
 * Main entry point for interacting with a running XD system.
 * 
 * @author Eric Bottard
 * @author Glenn Renfro
 * @author Ilayaperumal Gopinathan
 */
public interface SpringXDOperations {

	/**
	 * Returns the portion of the API for interacting with Streams.
	 */
	public StreamOperations streamOperations();

	/**
	 * Returns the portion of the API for interaction with Jobs.
	 */
	public JobOperations jobOperations();

	/**
	 * Returns the portion of the API for interaction with Modules' definitions.
	 */
	public ModuleOperations moduleOperations();

	/**
	 * Returns the portion of the API for interaction with runtime containers/modules.
	 */
	public RuntimeOperations runtimeOperations();

	/**
	 * Returns the portion of the API for interaction with Counters.
	 */
	public CounterOperations counterOperations();

	/**
	 * Returns the portion of the API for interaction with Field Value Counters.
	 */
	public FieldValueCounterOperations fvcOperations();

	/**
	 * Returns the portion of the API for interaction with Aggregate Counters.
	 */
	public AggregateCounterOperations aggrCounterOperations();

	/**
	 * Returns the portion of the API for interaction with Gauge.
	 */
	public GaugeOperations gaugeOperations();

	/**
	 * Returns the portion of the API for interaction with RichGauge.
	 */
	public RichGaugeOperations richGaugeOperations();

	/**
	 * Returns the portion of the API for providing code completion.
	 */
	public CompletionOperations completionOperations();

}
