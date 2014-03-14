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

	public Integer getPORT() {
		return httpPort;
	}

	public void setPORT(int httpPort) {
		this.httpPort = httpPort;
	}

}
