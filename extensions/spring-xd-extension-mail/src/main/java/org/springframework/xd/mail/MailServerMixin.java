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

package org.springframework.xd.mail;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.springframework.xd.module.options.spi.ModuleOption;


/**
 * Typical options for configuring connection to a mail server. Defines the following properties:
 * <ul>
 * <li>host (localhost)</li>
 * <li>port (25)</li>
 * <li>username (none)</li>
 * <li>password (none)</li>
 * </ul>
 * 
 */
public class MailServerMixin {

	private String host = "localhost";

	private int port = 25;

	private String username;

	private String password;

	@NotNull
	public String getHost() {
		return host;
	}

	@Min(0)
	@Max(65535)
	public int getPort() {
		return port;
	}


	public String getUsername() {
		return username;
	}


	public String getPassword() {
		return password;
	}

	@ModuleOption("the hostname of the mail server")
	public void setHost(String host) {
		this.host = host;
	}

	@ModuleOption("the port of the mail server")
	public void setPort(int port) {
		this.port = port;
	}

	@ModuleOption("the username to use to connect to the mail server")
	public void setUsername(String username) {
		this.username = username;
	}

	@ModuleOption("the password to use to connect to the mail server ")
	public void setPassword(String password) {
		this.password = password;
	}


}
