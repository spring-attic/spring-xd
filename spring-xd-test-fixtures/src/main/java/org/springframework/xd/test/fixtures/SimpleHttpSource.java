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

package org.springframework.xd.test.fixtures;

import java.io.File;

import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;
import org.springframework.xd.test.generator.GeneratorException;
import org.springframework.xd.test.generator.HttpGenerator;
import org.springframework.xd.test.generator.SimpleHttpGenerator;


/**
 * An HTTP source that default to using localhost:9000
 *
 * @author Glenn Renfro
 * @author Mark Pollack
 */
public class SimpleHttpSource extends AbstractModuleFixture<SimpleHttpSource> {

	private static final int DEFAULT_HTTP_PORT = 9000;

	private static final String DEFAULT_HTTP_HOST = "localhost";

	private volatile int port = DEFAULT_HTTP_PORT;

	private volatile String host = DEFAULT_HTTP_HOST;

	private volatile HttpGenerator httpGenerator;

	/**
	 * Construct a new SimpleHttpSource, given a host. The port will default to 9000.
	 */
	public SimpleHttpSource() {
		this(DEFAULT_HTTP_HOST, DEFAULT_HTTP_PORT);
	}

	/**
	 * Construct a new SimpleHttpSource, given a host. The port will default to 9000.
	 *
	 * @param host the host to connect to
	 */
	public SimpleHttpSource(String host) {
		this(host, DEFAULT_HTTP_PORT);
	}

	/***
	 * Construct a new SimpleHttpSource, given a host and a port connect to.
	 *
	 * @param host The host to connect to
	 * @param port The port to connect to
	 */
	public SimpleHttpSource(String host, int port) {
		Assert.hasText(host, "host must not be null or empty");
		this.port = port;
		this.host = host;
		httpGenerator = new SimpleHttpGenerator(host, port);
	}

	/**
	 * Ensure that the source is ready to take requests by sending http header requests to the source for up to 2
	 * seconds.
	 *
	 * @throws IllegalStateException if can't connect to the source within the given timeout.
	 */
	public SimpleHttpSource ensureReady() {
		return ensureReady(2000);
	}

	/**
	 * Ensure that the source is ready to take requests by sending http header requests for up to the timeout specified
	 * in millisecionds, sleeping 100 ms between attempts.
	 *
	 * @param timeoutInMillis
	 * @return a new SimpleHttpSource
	 * @throws IllegalStateException if can't connect to the source within the given timeout.
	 */
	public SimpleHttpSource ensureReady(int timeoutInMillis) {
		long giveUpAt = System.currentTimeMillis() + timeoutInMillis;
		String url = "http://" + host + ":" + port;
		while (System.currentTimeMillis() < giveUpAt) {
			try {
				new RestTemplate().headForHeaders(url);
				return this;
			}
			catch (Exception e) {
				try {
					Thread.sleep(100);
				}
				catch (InterruptedException e1) {
					Thread.currentThread().interrupt();
					throw new IllegalStateException(e1);
				}
			}
		}
		throw new IllegalStateException(String.format(
				"Source [%s] does not seem to be listening after waiting for %dms", this, timeoutInMillis));
	}

	@Override
	protected String toDSL() {
		return String.format("http --port=%d", port);
	}

	/**
	 * Make a HTTP POST request using the provided string in the http body.
	 *
	 * @param payload String to send in the http body.
	 */
	public void postData(String payload) {
		Assert.hasText(payload, "payload must not be empty nor null");

		httpGenerator.postData(payload);

	}

	/**
	 * Generate a http request from the contents of a file
	 *
	 * @param file the File that contains the data to post
	 * @throws GeneratorException If there was an error related to file handling.
	 */
	public void postFromFile(File file) {
		Assert.notNull(file, "file must not be null");

		httpGenerator.postFromFile(file);
	}

}
