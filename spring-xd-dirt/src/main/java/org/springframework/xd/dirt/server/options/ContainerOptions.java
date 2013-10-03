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

package org.springframework.xd.dirt.server.options;

import org.kohsuke.args4j.Option;

/**
 * A class the defines the options that will be parsed on the container command line.
 */
public class ContainerOptions extends AbstractOptions {

	/**
	 * @param defaultTransport
	 * @param defaultAnalytics
	 */
	public ContainerOptions() {
		super(Transport.redis, Analytics.redis);
	}


	@Option(name = "--" + STORE, usage = "How to persist container/modules data (default: redis)")
	private Store store = Store.redis;

	@Option(name = "--" + JMX_PORT, usage = "The JMX port for the container", metaVar = "<jmxPort>")
	private Integer jmxPort = 8779;

	@Override
	public Integer getJmxPort() {
		return jmxPort;
	}

	protected void setJmxPort(Integer jmxPort) {
		this.jmxPort = jmxPort;
	}

	@Override
	public Store getStore() {
		return store;
	}

	protected void setStore(Store store) {
		this.store = store;
	}
}
