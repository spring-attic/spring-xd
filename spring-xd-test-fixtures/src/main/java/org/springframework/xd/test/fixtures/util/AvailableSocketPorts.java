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

package org.springframework.xd.test.fixtures.util;

import java.io.IOException;
import java.net.Socket;

import org.springframework.integration.test.util.SocketUtils;


/**
 * Utility class to find available server ports.
 *
 * <p>
 * Leverages {@link SocketUtils#findAvailableServerSocket} but in addition remembers
 * the allocated ports. This is because the port may not be used immediately after
 * it is found and thus may be re-allocated on subsequent calls.
 * </p>
 *
 * @author Eric Bottard
 */
public class AvailableSocketPorts {

	private static int lastAllocatedPort = 1025;

	private AvailableSocketPorts() {
	}

	public static synchronized int nextAvailablePort() {
		int result = SocketUtils.findAvailableServerSocket(lastAllocatedPort);
		lastAllocatedPort = result + 1;
		if (lastAllocatedPort >= 65535) {
			// Hopefully this won't happen during a test suite, but...
			lastAllocatedPort = 1025;
		}
		return result;
	}

	/**
	 * Verifies that the port to the broker is listening. If not throws an IllegalStateException.
	 *
	 * @param fixtureName WThe module fixture is calling this method, used in case of exception
	 * @param host the host to connect to
	 * @param port the port to connect to
	 * @param timeout The max time to try to get the connection to the broker in milliseconds
	 *
	 * @throws IllegalStateException if can not connect in the specified timeout.
	 */
	public static void ensureReady(String fixtureName, String host, int port, int timeout) {
		long giveUpAt = System.currentTimeMillis() + timeout;
		while (System.currentTimeMillis() < giveUpAt) {
			try {
				new Socket(host, port);
				return;
			}
			catch (IOException e) {
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
				"Module [%s] does not seem to be listening after waiting for %dms", fixtureName, timeout));
	}

}
