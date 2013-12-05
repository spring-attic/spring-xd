/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.xd.shell.command.fixtures;

import java.io.File;

import org.springframework.shell.core.CommandResult;
import org.springframework.shell.core.JLineShellComponent;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;


/**
 * Represents an http source running on localhost.
 * 
 * @author Eric Bottard
 */
public class HttpSource extends AbstractModuleFixture {

	private int port;

	private JLineShellComponent shell;

	private String contentType;

	public HttpSource(JLineShellComponent shell) {
		this(shell, AvailableSocketPorts.nextAvailablePort());
	}

	public HttpSource(JLineShellComponent shell, int port) {
		this.port = port;
		this.shell = shell;
	}

	/**
	 * Attempts connections to the source until it is ready to accept data.
	 */
	public HttpSource ensureReady() {
		return ensureReady(2000);
	}

	public HttpSource ensureReady(int timeout) {
		long giveUpAt = System.currentTimeMillis() + timeout;
		while (System.currentTimeMillis() < giveUpAt) {
			try {
				new RestTemplate().headForHeaders("http://localhost:" + port);
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

	public HttpSource postData(String payload) {
		String command = String.format(
				"http post --target http://localhost:%d --data \"%s\"",
				port, payload);
		if (contentType != null) {
			command += String.format(" --contentType \"%s\"", contentType);
		}
		CommandResult result = shell.executeCommand(command);
		Assert.isTrue(result.isSuccess());
		return this;
	}

	public HttpSource useContentType(String contentType) {
		this.contentType = contentType;
		return this;
	}

	public HttpSource postFromFile(File file) {
		String command = String.format(
				"http post --target http://localhost:%d --file \"%s\"",
				port, file.getAbsolutePath());
		if (contentType != null) {
			command += String.format(" --contentType \"%s\"", contentType);
		}
		CommandResult result = shell.executeCommand(command);
		Assert.isTrue(result.isSuccess());
		return this;
	}

}
