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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.springframework.util.StreamUtils;
import org.springframework.xd.test.fixtures.util.AvailableSocketPorts;


/**
 * Test fixture that creates a simple socket server, ready to accept connections from a 'tcp' sink module.
 * 
 * @author Eric Bottard
 * @author Glenn Renfro
 */
public class TcpSink extends AbstractModuleFixture<TcpSink> implements Disposable {

	private static final int DEFAULT_TCP_PORT = 1234;

	private final int port;

	private ServerSocket serverSocket;

	private Socket clientSocket;

	private ByteArrayOutputStream baos = new ByteArrayOutputStream();

	private Thread listenerThread;

	private String host;

	/**
	 * Construct a TcpSink with a port selected by @link
	 * {@link org.springframework.xd.test.fixtures.util.AvailableSocketPorts#nextAvailablePort()}
	 */
	public TcpSink() {
		this(AvailableSocketPorts.nextAvailablePort());
	}

	/**
	 * Create a TcpSink with the provided port
	 * 
	 * @param host used to configure the sink
	 */
	public TcpSink(String host, int port) {
		this.host = host;
		this.port = port;
	}

	/**
	 * Create a TcpSink with the provided port
	 * 
	 * @param port used to configure the sink
	 */
	public TcpSink(int port) {
		this.port = port;
	}

	/**
	 * Construct a TcpSink with the specified host and a default port of 1234
	 * @param host the host where tcp data will be sent
	 * @return TcpSink
	 */
	public static TcpSink withDefaults(String host) {
		return new TcpSink(host, DEFAULT_TCP_PORT);
	}

	/**
	 * Construct a TcpSink with the default port of 1234
	 * 
	 * @return TcpSink
	 */
	public static TcpSink withDefaultPort() {
		return new TcpSink(DEFAULT_TCP_PORT);
	}

	/**
	 * Create a socket and copy received data into a buffer
	 * 
	 * @return instance of TcpSink for fluent API chaining
	 * @throws IOException socket processing errors
	 */
	public TcpSink start() throws IOException {
		serverSocket = new ServerSocket(port);
		listenerThread = new Thread() {

			@Override
			public void run() {
				try {
					clientSocket = serverSocket.accept();
					StreamUtils.copy(clientSocket.getInputStream(), baos);
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		};
		listenerThread.start();
		return this;
	}

	/**
	 * Return the bytes received by the sink
	 * 
	 * @return bytes received by the sink
	 * @throws IOException exception getting bytes from ByteArrayOutputStream
	 */
	public byte[] getReceivedBytes() throws IOException {
		return baos.toByteArray();
	}


	@Override
	public void cleanup() {
		try {
			if (clientSocket != null) {
				clientSocket.close();
			}
			if (serverSocket != null) {
				serverSocket.close();
			}
			listenerThread.interrupt();
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected String toDSL() {
		String result = String.format("tcp --port=%d ", port);
		if (host != null) {
			result = result + "--host=" + host;
		}
		return result;
	}

}
