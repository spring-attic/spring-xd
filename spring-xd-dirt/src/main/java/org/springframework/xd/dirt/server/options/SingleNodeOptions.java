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

package org.springframework.xd.dirt.server.options;

import javax.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.xd.dirt.server.options.CommonDistributedOptions.Store;


/**
 * Holds options that can be used in single-node mode. Some of those are hardcoded to accept a single value on purpose
 * because.
 * 
 * @author Eric Bottard
 * @author David Turanski
 */
@ConfigurationProperties
public class SingleNodeOptions {

	public static enum Analytics {
		memory, redis;
	}

	public static enum DataTransport {
		local, rabbit, redis;
	}

	public static enum ControlTransport {
		local, rabbit, redis;
	}

	private Analytics analytics;

	private DataTransport transport;

	private ControlTransport controlTransport;

	private Store store;

	@NotNull
	public Analytics getXD_ANALYTICS() {
		return analytics;
	}

	@NotNull
	public Store getXD_STORE() {
		return store;
	}

	@NotNull
	public DataTransport getXD_TRANSPORT() {
		return transport;
	}

	@NotNull
	public ControlTransport getXD_CONTROL_TRANSPORT() {
		return controlTransport;
	}

	public void setXD_ANALYTICS(Analytics analytics) {
		this.analytics = analytics;
	}

	public void setXD_STORE(Store store) {
		this.store = store;
	}

	public void setXD_TRANSPORT(DataTransport transport) {
		this.transport = transport;
	}

	public void setXD_CONTROL_TRANSPORT(ControlTransport controlTransport) {
		this.controlTransport = controlTransport;
	}
}
