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

import org.springframework.xd.rest.client.SpringXDOperations;
import org.springframework.xd.rest.client.StreamOperations;
import org.springframework.xd.rest.client.TapOperations;
import org.springframework.xd.rest.client.TriggerOperations;
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
	 * Holds the Tap-related part of the API.
	 */
	private TapOperations tapOperations;
	
	/**
	 * Holds the Trigger-related part of the API.
	 */
	private TriggerOperations triggerOperations;

	public SpringXDTemplate(URI baseURI) {
		XDRuntime xdRuntime = restTemplate.getForObject(baseURI, XDRuntime.class);
		resources.put("streams", URI.create(xdRuntime.getLink("streams").getHref()));
		resources.put("taps", URI.create(xdRuntime.getLink("taps").getHref()));
		resources.put("triggers", URI.create(xdRuntime.getLink("triggers").getHref()));
		streamOperations = new StreamTemplate(this);
		tapOperations = new TapTemplate(this);
		triggerOperations = new TriggerTemplate(this);
	}

	@Override
	public StreamOperations streamOperations() {
		return streamOperations;
	}

	@Override
	public TapOperations tapOperations() {
		return tapOperations;
	}
	
	@Override
	public TriggerOperations triggerOperations() {
		return triggerOperations;
	}

}
