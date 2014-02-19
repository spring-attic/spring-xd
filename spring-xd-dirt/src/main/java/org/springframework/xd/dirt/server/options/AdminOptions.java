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

import org.kohsuke.args4j.Option;

import org.springframework.boot.context.properties.ConfigurationProperties;


/**
 * Holds configuration options that are valid for the Admin server, when using distributed mode.
 * 
 * @author Eric Bottard
 */
@ConfigurationProperties
public class AdminOptions extends CommonDistributedOptions {

	@Option(name = "--httpPort", usage = "Http port for the REST API server", metaVar = "<httpPort>")
	private Integer httpPort;


	@Option(name = "--controlTransport", aliases = { "--control-transport", "--transport" }, usage = "The transport to use for control messages (between admin and nodes)")
	private ControlTransport controlTransport;

	public void setXD_CONTROL_TRANSPORT(ControlTransport controlTransport) {
		this.controlTransport = controlTransport;
	}

	@NotNull
	public ControlTransport getXD_CONTROL_TRANSPORT() {
		return controlTransport;
	}

	public Integer getPORT() {
		return httpPort;
	}

	public void setPORT(int httpPort) {
		this.httpPort = httpPort;
	}


}
