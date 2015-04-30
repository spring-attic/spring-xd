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

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.util.Assert;
import org.springframework.xd.test.fixtures.util.AvailableSocketPorts;


/**
 * A test fixture that allows testing of the mqtt source module.
 *
 * @author Glenn Renfro
 */
public class MqttSource extends AbstractModuleFixture<MqttSource> {

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

	/**
	 * Ensure that the Mqtt broker socket is available by polling it for up to 2 seconds
	 * @return MqttSource to use in fluent API chaining
	 * @throws IllegalStateException if can not connect in 2 seconds.
	 */
	public MqttSource ensureReady() {
		AvailableSocketPorts.ensureReady(this.getClass().getName(), host, port, 2000);
		return this;
	}

	/**
	 * Sends a string via Mqtt to a Rabbit Mqtt Broker.
	 *
	 * @param data String to be transmitted to the Mqtt Broker.
	 */
	public void sendData(String data) {
		Assert.hasText(data, "data must not be empty nor null");
		DefaultMqttPahoClientFactory factory;
		MqttClient client = null;
		factory = new DefaultMqttPahoClientFactory();
		factory.setPassword("guest");
		factory.setUserName("foobar");
		MqttMessage mqttMessage = new MqttMessage();
		mqttMessage.setPayload(data.getBytes());
		try {
			client = factory.getClientInstance("tcp://" + host + ":" + port, "guest");
			client.connect();
			client.publish("xd.mqtt.test", mqttMessage);
			try {
				Thread.sleep(1000);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException(e);
			}
			client.disconnect();
			client.close();
		}
		catch (MqttException mqttException) {
			throw new IllegalStateException(mqttException.getMessage());
		}
	}
}
