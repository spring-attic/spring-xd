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

import org.apache.activemq.ActiveMQConnectionFactory;

import org.springframework.jms.core.JmsTemplate;
import org.springframework.util.Assert;


/**
 * A test fixture that allows testing of the 'jms' source module.
 *
 * @author Glenn Renfro
 */
public class JmsSource extends AbstractModuleFixture {

	public static final int DEFAULT_PORT = 61616;

	protected int port;

	private String host;


	public JmsSource(String host, int port) {
		Assert.hasText(host, "host must not be empty nor null");

		this.host = host;
		this.port = port;
	}

	/**
	 * Generates a JmsSource instance using the default port 61616
	 *
	 * @param host The host machine where the JMS broker exists.
	 * @return a fuly qualified JmsSource fixture instance.
	 */
	public static JmsSource withDefaultPort(String host) {
		Assert.hasText(host, "host must not be empty nor null");

		return new JmsSource(host, DEFAULT_PORT);
	}

	/**
	 * Renders the DSL for this fixture.
	 */
	@Override
	protected String toDSL() {
		return "jms ";
	}

	/**
	 * Verifies that the port to the broker is available. If not throws an IllegalStateException. The timeout is set for
	 * 2 seconds.
	 *
	 * @return a reference to the mqtt source.
	 */
	public JmsSource ensureReady() {
		return ensureReady(2000);
	}

	/**
	 * Verifies that the port to the broker is available. If not throws an IllegalStateException.
	 *
	 * @param timeout The max time to try to get the connection to the broker.
	 * @return a reference to the mqtt source.
	 */
	public JmsSource ensureReady(int timeout) {
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

	/**
	 * Sends data to the JMS broker via TCP.
	 *
	 * @param data A string containing the data to send to the JMS broker.
	 */
	public void sendData(String data) {
		Assert.hasText(data, "data must not be empty nor null");

		JmsTemplate template = new JmsTemplate(new ActiveMQConnectionFactory("tcp://" + host + ":" + port));
		template.convertAndSend("ec2Test3", data);
	}

}
