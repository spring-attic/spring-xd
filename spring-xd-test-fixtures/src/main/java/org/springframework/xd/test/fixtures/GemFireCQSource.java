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

package org.springframework.xd.test.fixtures;

import org.springframework.util.Assert;

/**
 * Create a gemfire CQ (continuous query) source.
 *
 * @author David Turanski
 */
public class GemFireCQSource extends AbstractModuleFixture<GemFireCQSource> {
	private boolean useLocator;

	private String host = "localhost";

	private int port = 40404;

	private final String query;

	/**
	 * Create a Gemfire Sink module.
	 *
	 * @param query the OQL query string.
	 */
	public GemFireCQSource(String query) {
		super();
		Assert.hasText("query must not be empty nor null",query);
		this.query = query;
	}

	/**
	 * Set the host and port to reference a Gemfire locator instead of a server node.
	 *
	 * @param useLocator true if using a locator. Default is false.
	 * @return this
	 */
	public GemFireCQSource useLocator(boolean useLocator) {
		this.useLocator = useLocator;
		return this;
	}

	/**
	 * Set the host name
	 *
	 * @param host the host. Default is localhost
	 * @return this
	 */
	public GemFireCQSource host(String host) {
		Assert.hasLength(host, "'host' must not be empty or null");
		this.host = host;
		return this;
	}

	/**
	 * Set the gemfire server port. Default is 40404
	 *
	 * @param port the port
	 * @return this
	 */
	public GemFireCQSource port(int port) {
		this.port = port;
		return this;
	}

	@Override
	protected String toDSL() {
		StringBuilder dslBuilder = new StringBuilder();
		dslBuilder.append("gemfire-cq");
		dslBuilder.append(" --useLocator=" + useLocator);
		dslBuilder.append(" --host=" + host);
		dslBuilder.append(" --port=" + port);
		dslBuilder.append(" --query=" + query);

		return dslBuilder.toString();
	}
}
