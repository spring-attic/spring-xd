/*
 * Copyright 2013-2014 the original author or authors.
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

import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.Range;

import org.springframework.xd.module.options.spi.ModuleOption;

/**
 * Factors out options for the typical connection setup to RabbitMQ.
 * 
 * @author Eric Bottard
 * @author Gary Russell
 */
public class RabbitConnectionMixin {

	private String host = "localhost";

	private int port = 5672;

	private String vhost = "/";

	private String username = "guest";

	private String password = "guest";

	private String addresses;

	@NotBlank
	public String getUsername() {
		return username;
	}

	@ModuleOption("the username to use to connect to the broker")
	public void setUsername(String username) {
		this.username = username;
	}

	@NotBlank
	public String getPassword() {
		return password;
	}

	@ModuleOption("the password to use to connect to the broker")
	public void setPassword(String password) {
		this.password = password;
	}

	@NotBlank
	public String getHost() {
		return host;
	}

	@ModuleOption("the host (or IP Address) to connect to")
	public void setHost(String host) {
		this.host = host;
	}

	@Range(min = 1, max = 65535)
	public int getPort() {
		return port;
	}

	@ModuleOption("the port to connect to")
	public void setPort(int port) {
		this.port = port;
	}

	public String getAddresses() {
		if (this.addresses == null) {
			return this.host + ":" + this.port;
		}
		else {
			return this.addresses;
		}
	}

	@ModuleOption("the list of 'host:port' addresses; comma separated - overrides host, port")
	public void setAddresses(String addresses) {
		this.addresses = addresses;
	}

	public String getVhost() {
		return vhost;
	}

	@ModuleOption("the RabbitMQ virtual host to use")
	public void setVhost(String vhost) {
		this.vhost = vhost;
	}

}
