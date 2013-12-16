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

package org.springframework.xd.dirt.modules.metadata;

import javax.validation.constraints.Size;

import org.springframework.xd.module.options.spi.ModuleOption;
import org.springframework.xd.module.options.spi.SourceModuleOptionsMetadataSupport;

/**
 * Describes options to the {@code rabbit} source module.
 * 
 * @author Eric Bottard
 */
public class RabbitSourceOptionsMetadata extends SourceModuleOptionsMetadataSupport {

	private String host;

	// Using wrapper here b/c default comes from properties file for now
	private Integer port;

	private String vhost;

	private String queues;

	@Size
	public String getHost() {
		return host;
	}

	@ModuleOption("the host (or IP Address) to connect to")
	public void setHost(String host) {
		this.host = host;
	}


	public Integer getPort() {
		return port;
	}

	@ModuleOption("the port to connect to")
	public void setPort(int port) {
		this.port = port;
	}


	public String getVhost() {
		return vhost;
	}


	@ModuleOption("the RabbitMQ virtual host to use")
	public void setVhost(String vhost) {
		this.vhost = vhost;
	}


	public String getQueues() {
		return queues;
	}


	@ModuleOption("the queue(s) from which messages will be received")
	public void setQueues(String queues) {
		this.queues = queues;
	}


}
