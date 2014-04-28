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

package org.springframework.xd.integration.test;

import java.util.UUID;

import org.junit.Test;

import org.springframework.xd.test.fixtures.SimpleFileSink;


/**
 * Runs a basic suite of JMS Source tests on an XD Cluster instance.
 * 
 * @author Glenn Renfro
 */
public class MqttTest extends AbstractIntegrationTest {


	/**
	 * Verifies that the MQTT Source that terminates with a CRLF returns the correct data.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testMqttSource() throws Exception {
		String data = UUID.randomUUID().toString();
		stream(sources.mqtt() + XD_DELIMETER + sinks.getSink(SimpleFileSink.class));
		sources.mqtt().ensureReady();
		sources.mqtt().sendData(data);
		waitForXD(2000);
		assertReceived(1);
		assertValid(data, sinks.getSink(SimpleFileSink.class));
	}

	@Test
	public void testMqttSink() throws Exception {
		String data = UUID.randomUUID().toString();
		stream(sources.mqtt() + XD_DELIMETER + sinks.getSink(SimpleFileSink.class));
		waitForXD(2000);
		stream("mqttSender", "trigger --payload='" + data + "'" + XD_DELIMETER + sinks.mqtt());
		waitForXD(2000);
		assertReceived(1);
		assertValid(data, sinks.getSink(SimpleFileSink.class));

	}

}
