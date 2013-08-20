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

package org.springframework.xd.shell.command;

import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;

import org.eclipse.jdt.internal.core.Assert;

import org.springframework.integration.test.util.SocketUtils;
import org.springframework.shell.core.CommandResult;
import org.springframework.shell.core.JLineShellComponent;


/**
 * Represents an http source running on localhost.
 *
 * @author Eric Bottard
 */
public class HttpSource extends AbstractModuleFixture {

	private int port;

	private JLineShellComponent shell;

	public HttpSource(JLineShellComponent shell) {
		this(shell, SocketUtils.findAvailableServerSocket(8000));
	}

	public HttpSource(JLineShellComponent shell, int port) {
		this.port = port;
		this.shell = shell;
	}

	/**
	 * Attempts connections to the source until it is ready to accept data.
	 */
	public void ensureReady() {
		ensureReady(2000);
	}

	public void ensureReady(int timeout) {
		long giveUpAt = System.currentTimeMillis() + timeout;
		while (System.currentTimeMillis() < giveUpAt) {
			try {
				Socket socket = new Socket("localhost", port);
				socket.close();
				return;
			}
			catch (ConnectException ignore) {
				try {
					Thread.sleep(100);
				}
				catch (InterruptedException e) {
				}
			}
			catch (UnknownHostException e) {
				throw new IllegalStateException("Should never happen", e);
			}
			catch (IOException e) {
				throw new IllegalStateException("Should never happen", e);
			}
		}
		throw new IllegalStateException(String.format("Source [%s] does not seem to be listening after waiting for %dms", this, timeout));
	}

	@Override
	protected String toDSL() {
		return String.format("http --port=%d", port);
	}

	public void postData(String payload) {
		CommandResult result = shell.executeCommand(String.format("http post --target http://localhost:%d --data %s", port, payload));
		Assert.isTrue(result.isSuccess());
	}

}
