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
 * Create a gemfire source
 *
 * @author David Turanski
 */
public class GemFireSource extends AbstractModuleFixture<GemFireSource> {
	private boolean useLocator;

	private String host = "localhost";

	private int port = 40404;

	private final String region;

	private String cacheEventExpression;

	/**
	 * Create a Gemfire Sink module.
	 *
	 * @param region the name of the region to use for this stream. The region must exist on the server.
	 */
	public GemFireSource(String region) {
		super();
		Assert.hasText("region must not be empty nor null",region);
		this.region = region;
	}

	/**
	 * Set the host and port to reference a Gemfire locator instead of a server node.
	 *
	 * @param useLocator true if using a locator. Default is false.
	 * @return this
	 */
	public GemFireSource useLocator(boolean useLocator) {
		this.useLocator = useLocator;
		return this;
	}

	/**
	 * Set the host name
	 *
	 * @param host the host. Default is localhost
	 * @return this
	 */
	public GemFireSource host(String host) {
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
	public GemFireSource port(int port) {
		this.port = port;
		return this;
	}

	/**
	 * Set the cache key. Defaults to stream name
	 *
	 * @param cacheEventExpression a SpEL expression evaluating the payload
	 * @return this
	 */
	public GemFireSource cacheEventExpression(String cacheEventExpression) {
		this.cacheEventExpression = cacheEventExpression;
		return this;
	}

	@Override
	protected String toDSL() {
		StringBuilder dslBuilder = new StringBuilder();
		dslBuilder.append("gemfire");
		dslBuilder.append(" --useLocator=" + useLocator);
		dslBuilder.append(" --host=" + host);
		dslBuilder.append(" --port=" + port);
		dslBuilder.append(" --regionName=" + region);
		if (cacheEventExpression != null) {
			dslBuilder.append(" --cacheEventExpression=" + cacheEventExpression);
		}

		return dslBuilder.toString();
	}
}
