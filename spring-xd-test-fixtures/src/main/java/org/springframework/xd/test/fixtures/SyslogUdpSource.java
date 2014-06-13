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
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import org.springframework.util.Assert;


/**
 * A test fixture that allows testing of the syslog udp source module.
 *
 * @author Glenn Renfro
 */
public class SyslogUdpSource extends AbstractModuleFixture<SyslogUdpSource> {

	public final static int DEFAULT_PORT = 5140;

	public final static String DEFAULT_HOST = "localhost";

	private int port;

	private String host;

	/**
	 * Initializes a SyslogUdpSource fixture.
	 *
	 * @param host the host that udp data will be sent.
	 * @param port the port that udp data will be sent.
	 */
	public SyslogUdpSource(String host, int port) {
		Assert.hasText(host, "host must not be empty nor null");
		this.host = host;
		this.port = port;
	}

	/**
	 * Returns a SyslogUdpSource fixture instance that is initialized with default port and host.
	 *
	 * @return a SyslogUdpSource instance.
	 */
	public static SyslogUdpSource withDefaults() {
		return new SyslogUdpSource(DEFAULT_HOST, DEFAULT_PORT);
	}

	/**
	 * Returns a SyslogUdpSource fixture instance that is initialized with default port and the host param.
	 *
	 * @param host The host that udp will be sent.
	 * @return a SyslogUdpSource instance
	 */
	public static SyslogUdpSource withDefaults(String host) {
		Assert.hasText(host, "host must not be empty nor null");
		return new SyslogUdpSource(host, DEFAULT_PORT);
	}

	/**
	 * Renders the DSL for this fixture.
	 */
	@Override
	protected String toDSL() {
		return String.format("syslog-udp --port=%s ", port);
	}

	/**
	 * Sends UDP traffic to a syslog source host and port.
	 *
	 * @param bytes the data to be sent.
	 */
	public void sendBytes(byte[] bytes) {
		DatagramSocket clientSocket = null;
		try {
			clientSocket = new DatagramSocket();

			InetAddress IPAddress = InetAddress.getByName(host);
			DatagramPacket sendPacket =
					new DatagramPacket(bytes, bytes.length, IPAddress, port);
			clientSocket.send(sendPacket);
			clientSocket.close();
		}
		catch (SocketException se) {
			throw new IllegalStateException(se.getMessage(), se);
		}
		catch (IOException ioe) {
			throw new IllegalStateException(ioe.getMessage(), ioe);
		}
		finally {
			if (clientSocket != null) {
				clientSocket.close();
			}
		}

	}


}
