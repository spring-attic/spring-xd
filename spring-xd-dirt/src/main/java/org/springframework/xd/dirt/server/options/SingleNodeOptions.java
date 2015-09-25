/*
 * Copyright 2013-2014 the original author or authors.
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

import org.kohsuke.args4j.Option;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.xd.dirt.server.options.ResourcePatternScanningOptionHandlers.SingleNodeAnalyticsOptionHandler;
import org.springframework.xd.dirt.server.options.ResourcePatternScanningOptionHandlers.SingleNodeDataTransportOptionHandler;

/**
 * Holds options that can be used in single-node mode. Some of those are hardcoded to accept a single value on purpose
 * because.
 * 
 * @author Eric Bottard
 * @author David Turanski
 */
@ConfigurationProperties
public class SingleNodeOptions extends CommonOptions {

	@Option(name = "--analytics", handler = SingleNodeAnalyticsOptionHandler.class,
			usage = "How to persist analytics such as counters and gauges")
	private String analytics;

	@Option(name = "--transport", handler = SingleNodeDataTransportOptionHandler.class,
			usage = "The transport to use for data messages (between modules within a stream)")
	private String transport;

	@Option(name = "--httpPort", usage = "HTTP port for the REST API server", metaVar = "<httpPort>")
	private Integer httpPort;

	public Integer getPORT() {
		return httpPort;
	}

	public void setPORT(int httpPort) {
		this.httpPort = httpPort;
	}

	@NotNull
	public String getXD_ANALYTICS() {
		return analytics;
	}

	@NotNull
	public String getXD_TRANSPORT() {
		return transport;
	}

	public void setXD_ANALYTICS(String analytics) {
		this.analytics = analytics;
	}

	public void setXD_TRANSPORT(String transport) {
		this.transport = transport;
	}
}
