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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.springframework.util.StreamUtils;


/**
 * Test fixture that creates a simple socket server, ready to accept connections from a 'tcp' sink module.
 * 
 * @author Eric Bottard
 */
public class TcpSink extends AbstractModuleFixture implements Disposable {

	protected int port = AvailableSocketPorts.nextAvailablePort();

	private ServerSocket serverSocket;

	private Socket clientSocket;

	ByteArrayOutputStream baos = new ByteArrayOutputStream();

	private Thread listenerThread;

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
		return String.format("tcp --port=%d", port);
	}

}
