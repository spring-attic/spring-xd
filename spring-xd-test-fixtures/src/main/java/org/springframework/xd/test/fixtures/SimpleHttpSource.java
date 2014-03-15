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

import org.springframework.web.client.RestTemplate;
import org.springframework.xd.test.generator.HttpGenerator;
import org.springframework.xd.test.generator.SimpleHttpGenerator;


/**
 * 
 * @author Glenn Renfro
 */
public class SimpleHttpSource extends AbstractModuleFixture {

	private int port;

	private String host;

	private HttpGenerator httpGenerator;

	public SimpleHttpSource(int port) throws Exception {
		this.port = port;
		host = "localhost";
		httpGenerator = new SimpleHttpGenerator(host, port);

	}

	public SimpleHttpSource(String host, int port) throws Exception {
		this.port = port;
		this.host = host;
		httpGenerator = new SimpleHttpGenerator(host, port);

	}

	/**
	 * Attempts connections to the source until it is ready to accept data.
	 */
	public SimpleHttpSource ensureReady() {
		return ensureReady(2000);
	}

	public SimpleHttpSource ensureReady(int timeout) {
		long giveUpAt = System.currentTimeMillis() + timeout;
		while (System.currentTimeMillis() < giveUpAt) {
			try {
				new RestTemplate().headForHeaders("http://" + host + ":" + port);
				return this;
			}
			catch (Exception e) {
				try {
					Thread.sleep(100);
				}
				catch (InterruptedException e1) {
					Thread.currentThread().interrupt();
				}
			}
		}
		throw new IllegalStateException(String.format(
				"Source [%s] does not seem to be listening after waiting for %dms", this, timeout));
	}

	@Override
	protected String toDSL() {
		return String.format("http --port=%d", port);
	}

	public void postData(String payload) throws Exception {
		httpGenerator.postData(payload);

	}

	public void postFromFile(File file) throws Exception {
		httpGenerator.postFromFile(file);
	}

}
