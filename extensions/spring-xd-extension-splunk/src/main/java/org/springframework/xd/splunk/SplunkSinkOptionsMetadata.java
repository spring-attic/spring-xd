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

package org.springframework.xd.splunk;

import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.Range;

import org.springframework.xd.module.options.spi.ModuleOption;


/**
 * Captures options to the {@code splunk} sink module.
 * 
 * @author Eric Bottard
 */
public class SplunkSinkOptionsMetadata {

	private int tcpPort = 9500;

	private String host = "localhost";

	private int port = 8089;

	private String username = "admin";

	private String password = "password";

	private String owner = "admin";

	@Range(min = 0, max = 65536)
	public int getTcpPort() {
		return tcpPort;
	}

	@NotBlank
	public String getHost() {
		return host;
	}

	@Range(min = 0, max = 65536)
	public int getPort() {
		return port;
	}

	@NotBlank
	public String getUsername() {
		return username;
	}

	@NotBlank
	public String getPassword() {
		return password;
	}

	@NotBlank
	public String getOwner() {
		return owner;
	}

	@ModuleOption("the TCP port number to where XD will send the data")
	public void setTcpPort(int tcpPort) {
		this.tcpPort = tcpPort;
	}

	@ModuleOption("the host name or IP address of the Splunk server")
	public void setHost(String host) {
		this.host = host;
	}

	@ModuleOption("the TCP port number of the Splunk server")
	public void setPort(int port) {
		this.port = port;
	}

	@ModuleOption("the login name that has rights to send data to the tcpPort")
	public void setUsername(String username) {
		this.username = username;
	}

	@ModuleOption("the password associated with the username")
	public void setPassword(String password) {
		this.password = password;
	}

	@ModuleOption("the owner of the tcpPort")
	public void setOwner(String owner) {
		this.owner = owner;
	}

}
