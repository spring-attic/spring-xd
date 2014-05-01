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

package org.springframework.xd.test.fixtures;

import java.io.IOException;
import java.net.Socket;


/**
 * A test fixture that allows testing of the 'tcp' source module.
 *
 * @author Eric Bottard
 */
public class TcpSource extends AbstractModuleFixture {

	private static final int DEFAULT_TCP_PORT = 1234;

	protected final int port;

	private String host;

	/**
	 * Construct a new TcpSource with the loopback address for host and using a port selected by @link
	 * {@link org.springframework.xd.test.fixtures.AvailableSocketPorts#nextAvailablePort()}
	 */
	public TcpSource() {
		this(null);
	}

	/**
	 * Construct a new TcpSource with the provided host and using a port selected by @link
	 * {@link org.springframework.xd.test.fixtures.AvailableSocketPorts#nextAvailablePort()}
	 *
	 * @param host the host to connect to
	 */
	public TcpSource(String host) {
		this(host, AvailableSocketPorts.nextAvailablePort());
	}

	/**
	 * Construct a new TcpSource with the provided host and port
	 *
	 * @param host host to connect to
	 * @param port port to connect to
	 */
	public TcpSource(String host, int port) {
		// Note, null is allowed for host since when creating a socket will default to loopback address
		this.host = host;
		this.port = port;
	}

	/**
	 * Construct a new TcpSource using the provided host and the default port of 1234
	 *
	 * @param host host to connect to
	 * @return TcpSource to use
	 */
	public static TcpSource withDefaultPort(String host) {
		return new TcpSource(host, DEFAULT_TCP_PORT);
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

	public void sendBytes(byte[] bytes) {
		Socket socket = null;
		try {
			socket = new Socket(host, port);
			socket.getOutputStream().write(bytes);
		}
		catch (IOException ioException) {
			throw new IllegalStateException(ioException.getMessage());
		}
		finally {
			if (socket != null) {
				try {
					socket.close();
				}
				catch (IOException ioException) {
					// no action required
				}
			}
		}
	}

}
