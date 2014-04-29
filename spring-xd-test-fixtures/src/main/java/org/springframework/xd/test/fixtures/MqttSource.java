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

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.util.Assert;


/**
 * A test fixture that allows testing of the 'jms' source module.
 * 
 * @author Glenn Renfro
 */
public class MqttSource extends AbstractModuleFixture {

	private static final int DEFAULT_MQTT_PORT = 1883;

	private final int port;

	private final String host;

	public MqttSource(String host) {
		this(host, DEFAULT_MQTT_PORT);
	}

	public MqttSource(String host, int port) {
		Assert.notNull(host, "host must not be null or empty");
		this.host = host;
		this.port = port;
	}


	@Override
	protected String toDSL() {
		return "mqtt --url='tcp://" + host + ":" + port + "' --topics='xd.mqtt.test'";
	}

	public MqttSource ensureReady() {
		return ensureReady(2000);
	}

	public MqttSource ensureReady(int timeout) {
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
		DefaultMqttPahoClientFactory factory;
		MqttClient client = null;
		factory = new DefaultMqttPahoClientFactory();
		factory.setPassword("guest");
		factory.setUserName("foobar");
		MqttMessage mqttMessage = new MqttMessage();
		mqttMessage.setPayload(data.getBytes());
		client = factory.getClientInstance("tcp://" + host + ":" + port, "guest");
		client.connect();
		client.publish("xd.mqtt.test", mqttMessage);
		try {
			Thread.sleep(1000);
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
		client.disconnect();
		client.close();

	}
}
