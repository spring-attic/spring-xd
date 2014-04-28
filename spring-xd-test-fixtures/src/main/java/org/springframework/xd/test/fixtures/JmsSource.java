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


/**
 * A test fixture that allows testing of the 'jms' source module.
 * 
 * @author Glenn Renfro
 */
public class JmsSource extends AbstractModuleFixture {

	protected int port = 61616;

	private String host;


	public JmsSource() {

	}

	public JmsSource(String host, int port) {
		this.host = host;
		this.port = port;
	}


	@Override
	protected String toDSL() {
		return "jms ";
	}

	public JmsSource ensureReady() {
		return ensureReady(2000);
	}

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


	public void sendData(String data) throws Exception {
		System.out.println("tcp://" + host + ":" + port);
		JmsTemplate template = new JmsTemplate(new ActiveMQConnectionFactory("tcp://" + host + ":" + port));
		template.convertAndSend("ec2Test3", data);
	}
}
