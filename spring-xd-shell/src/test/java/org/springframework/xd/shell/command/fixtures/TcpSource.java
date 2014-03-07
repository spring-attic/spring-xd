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

import java.io.IOException;
import java.net.Socket;


/**
 * A test fixture that allows testing of the 'tcp' source module.
 * 
 * @author Eric Bottard
 */
public class TcpSource extends AbstractModuleFixture {

	protected int port = AvailableSocketPorts.nextAvailablePort();

	private String host;


	public TcpSource() {

	}

	public TcpSource(String host, int port) {
		this.host = host;
		this.port = port;
	}


	@Override
	protected String toDSL() {
		return String.format("tcp --port=%d", port);
	}

	public TcpSource ensureReady() {
		return ensureReady(2000);
	}

	public TcpSource ensureReady(int timeout) {
		long giveUpAt = System.currentTimeMillis() + timeout;
		while (System.currentTimeMillis() < giveUpAt) {
			try {
				new Socket(host, port);
				return this;
			}
			catch (IOException e) {
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

	public void sendBytes(byte[] bytes) throws IOException {
		Socket socket = new Socket(host, port);
		try {
			socket.getOutputStream().write(bytes);
		}
		finally {
			if (socket != null) {
				socket.close();
			}
		}
	}

}
