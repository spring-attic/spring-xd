/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.xd.dirt.modules.metadata;

import org.springframework.xd.module.options.spi.ModuleOption;


/**
 * Mixin for Redis connection options.
 *
 * @author Ilayaperumal Gopinathan
 */
public class RedisConnectionMixin {

	private String hostname = "localhost";

	private int port = 6379;

	private String password = "";

	public String getHostname() {
		return hostname;
	}

	@ModuleOption("redis host name")
	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public int getPort() {
		return port;
	}

	@ModuleOption("redis port")
	public void setPort(int port) {
		this.port = port;
	}

	public String getPassword() {
		return password;
	}

	@ModuleOption("redis password")
	public void setPassword(String password) {
		this.password = password;
	}

}
