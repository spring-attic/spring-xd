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


import java.io.IOException;
import java.net.Socket;

import org.springframework.util.Assert;


/**
 * A test fixture that allows testing of the syslog tcp source module.
 *
 * @author Glenn Renfro
 */
public class SyslogTcpSource extends AbstractModuleFixture<SyslogTcpSource> {

	public final static int DEFAULT_PORT = 5140;

	public final static String DEFAULT_HOST = "localhost";

	private int port;

	private String host;

	/**
	 * Initializes a SyslogTcpSource fixture.
	 *
	 * @param host the host that tcp data will be sent.
	 * @param port the port that tcp data will be sent.
	 */
	public SyslogTcpSource(String host, int port) {
		Assert.hasText(host, "host must not be empty nor null");
		this.host = host;
		this.port = port;
	}

	/**
	 * Returns a SyslogTcpSource fixture instance that is initialized with default port and host.
	 *
	 * @return a SyslogTcpSource instance.
	 */
	public static SyslogTcpSource withDefaults() {
		return new SyslogTcpSource(DEFAULT_HOST, DEFAULT_PORT);
	}

	/**
	 * Returns a SyslogTcpSource fixture instance that is initialized with default port and the 
	 * provided host parameter
	 *
	 * @param host The host that tcp data will be sent.
	 * @return a SyslogTcpSource instance
	 */
	public static SyslogTcpSource withDefaults(String host) {
		Assert.hasText(host, "host must not be empty nor null");
		return new SyslogTcpSource(host, DEFAULT_PORT);
	}

	/**
	 * Renders the DSL for this fixture.
	 */
	@Override
	protected String toDSL() {
		return String.format("syslog-tcp --port=%s ", port);
	}

	/**
	 * Sends TCP traffic to a syslog source host and port.
	 *
	 * @param bytes the data to be sent.
	 */
	public void sendBytes(byte[] bytes) {
		Socket socket = null;
		try {
			socket = new Socket(host, port);
			socket.getOutputStream().write(bytes);
			socket.close();
		}
		catch (IOException ioe) {
			throw new IllegalStateException(ioe.getMessage(), ioe);
		}
		finally {
			try {
				if (socket != null) {
					socket.close();
				}
			}
			catch (IOException ioe) {
			}
		}
	}

}
