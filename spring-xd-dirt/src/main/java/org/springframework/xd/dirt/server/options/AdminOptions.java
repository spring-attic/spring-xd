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
 * A class the defines the options that will be parsed on the admin command line.
 * 
 * @author Mark Pollack
 * @author David Turanski
 */
public class AdminOptions extends AbstractOptions {

	private static final String STORE = "store";

	private static final String HTTP_PORT = "httpPort";

	public AdminOptions() {
		super(Transport.redis, Analytics.redis);
	}

	protected AdminOptions(Transport defaultTransport, Analytics defaultAnalytics) {
		super(defaultTransport, defaultAnalytics);
	}

	@Option(name = "--" + HTTP_PORT, usage = "Http port for the REST API server (default: 8080)", metaVar = "<httpPort>")
	protected int httpPort = 8080;

	@Option(name = "--" + STORE, usage = "How to persist admin data (default: redis)")
	protected Store store = Store.redis;

	@Option(name = "--" + JMX_PORT, usage = "The JMX port for the admin", metaVar = "<jmxPort>")
	protected Integer jmxPort = 8778;

	/**
	 * @return http port
	 */
	public Integer getHttpPort() {
		return httpPort;
	}

	public Store getStore() {
		return store;
	}

	@Override
	public Integer getJmxPort() {
		return jmxPort;
	}

	@Override
	protected void createOptionMetadataCache() {
		super.createOptionMetadataCache();
		optionMetadataCache.put(getHttpPort(), isArg(HTTP_PORT));
		optionMetadataCache.put(getStore(), isArg(STORE));
	}
}
