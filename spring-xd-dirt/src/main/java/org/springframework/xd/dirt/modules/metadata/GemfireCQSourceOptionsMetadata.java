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

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import org.hibernate.validator.constraints.NotBlank;
import org.springframework.xd.module.options.spi.ModuleOption;

/**
 * Describes options to the {@code gemfire-cq} source module.
 * 
 * @author Eric Bottard
 */
public class GemfireCQSourceOptionsMetadata {

	private int port = 40404;

	private String host = "localhost";

	private String query;


	@Min(0)
	@Max(65535)
	public int getPort() {
		return port;
	}

	@ModuleOption("the port on which the GemFire server is running")
	public void setPort(int port) {
		this.port = port;
	}

	@NotBlank
	public String getHost() {
		return host;
	}


	@ModuleOption("the host on which the GemFire server is running")
	public void setHost(String host) {
		this.host = host;
	}


	@NotBlank
	public String getQuery() {
		return query;
	}

	@ModuleOption("the query string in Object Query Language (OQL)")
	public void setQuery(String query) {
		this.query = query;
	}


}
