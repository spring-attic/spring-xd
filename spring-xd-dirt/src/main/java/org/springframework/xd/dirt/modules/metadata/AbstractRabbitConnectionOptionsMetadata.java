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

/**
 * Factors out options for the typical connection setup to RabbitMQ.
 * 
 * @author Eric Bottard
 */
public abstract class AbstractRabbitConnectionOptionsMetadata {

	private String host;

	private Integer port;

	private String vhost;

	private String username;

	private String password;

	public String getUsername() {
		return username;
	}

	@ModuleOption("the username to use to connect to the broker")
	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	@ModuleOption("the password to use to connect to the broker")
	public void setPassword(String password) {
		this.password = password;
	}

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

}
