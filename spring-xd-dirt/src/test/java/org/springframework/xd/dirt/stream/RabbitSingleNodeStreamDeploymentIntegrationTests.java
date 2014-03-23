/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.xd.dirt.stream;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.ExternalResource;

import org.springframework.integration.x.bus.RabbitTestMessageBus;
import org.springframework.xd.dirt.integration.test.sink.NamedChannelSink;
import org.springframework.xd.dirt.integration.test.sink.SingleNodeNamedChannelSinkFactory;
import org.springframework.xd.dirt.integration.test.source.NamedChannelSource;
import org.springframework.xd.dirt.integration.test.source.SingleNodeNamedChannelSourceFactory;
import org.springframework.xd.test.mqtt.MqttTestSupport;
import org.springframework.xd.test.rabbit.RabbitTestSupport;

/**
 * @author Mark Fisher
 * @author Gary Russell
 */
public class RabbitSingleNodeStreamDeploymentIntegrationTests extends
		AbstractSingleNodeStreamDeploymentIntegrationTests {

	@ClassRule
	public static RabbitTestSupport rabbitAvailableRule = new RabbitTestSupport();

	@ClassRule
	public static MqttTestSupport mqttAvailableRule = new MqttTestSupport();

	@BeforeClass
	public static void setUp() {
		setUp("rabbit");
	}

	@ClassRule
	public static ExternalResource initializeRabbitTestMessageBus = new ExternalResource() {

		@Override
		protected void before() {
			if (testMessageBus == null || !(testMessageBus instanceof RabbitTestMessageBus)) {
				testMessageBus = new RabbitTestMessageBus(rabbitAvailableRule.getResource(), getCodec());
			}
		}
	};

	@Test
	public void mqttSourceStreamReceivesMqttSinkStreamOutput() throws Exception {
		StreamDefinition mqtt1 = new StreamDefinition("mqtt1", "queue:mqttsource > mqtt --topic=foo");
		StreamDefinition mqtt2 = new StreamDefinition("mqtt2", "mqtt --topics=foo > queue:mqttsink");
		integrationSupport.createAndDeployStream(mqtt1);
		integrationSupport.createAndDeployStream(mqtt2);

		NamedChannelSource source = new SingleNodeNamedChannelSourceFactory(integrationSupport.messageBus()).createNamedChannelSource("queue:mqttsource");
		NamedChannelSink sink = new SingleNodeNamedChannelSinkFactory(integrationSupport.messageBus()).createNamedChannelSink("queue:mqttsink");

		Thread.sleep(1000);
		source.sendPayload("hello");
		Object result = sink.receivePayload(1000);

		assertEquals("hello", result);
		source.unbind();
		sink.unbind();
	}
}
