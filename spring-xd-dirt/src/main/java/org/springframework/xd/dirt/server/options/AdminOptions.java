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
 * @author Mark Pollack
 */
public class AdminOptions extends AbstractOptions {

	@Option(name = "--httpPort", usage = "Http port for the REST API server (default: 8080)", metaVar = "<httpPort>")
	private int httpPort = 8080;

	@Option(name = "--store", usage = "How to persist admin data (default: redis)")
	private Store store = Store.redis;

	@Option(name = "--jmxPort", usage = "The JMX port for the admin", metaVar = "<jmxPort>")
	private int jmxPort = 8778;

	@Option(name = "--hadoopDistro", usage = "The Hadoop distro to use (default: hadoop10)")
	private HadoopDistro hadoopDistro = HadoopDistro.hadoop10;

	/**
	 * @return http port
	 */
	public int getHttpPort() {
		return httpPort;
	}

	public Store getStore() {
		return store;
	}

	public static void setXDStore(Store store) {
		System.setProperty("xd.store", store.name());
	}

	@Override
	public int getJmxPort() {
		return this.jmxPort;
	}

}
