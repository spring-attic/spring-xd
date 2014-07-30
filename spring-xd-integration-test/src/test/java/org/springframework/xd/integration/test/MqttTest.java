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


/**
 * Runs a basic suite of Mqtt Source and Sink tests on an XD Cluster instance.
 *
 * @author Glenn Renfro
 */
public class MqttTest extends AbstractIntegrationTest {


	/**
	 * Verifies that data sent to a MQTT Broker can be retrieved by the mqtt source. The data retrieved by the broker is
	 * then written to file, and the content of the file is verified against the original data set.
	 */
	@Test
	public void testMqttSource() {
		String data = UUID.randomUUID().toString();
		stream(sources.mqtt() + XD_DELIMITER + sinks.file());
		sources.mqtt().ensureReady();
		sources.mqtt().sendData(data);
		assertValid(data, sinks.file());
		assertReceived(1);
	}

	/**
	 * Verifies that the data sent from a MQTT Sink was received by a broker. This method utilizes mqttsource to
	 * retrieve data from the broker. This data is then written to file, and the content of the file is verified against
	 * the original data set.
	 */
	@Test
	public void testMqttSink() {
		String data = UUID.randomUUID().toString();
		stream(sources.mqtt() + XD_DELIMITER + sinks.file());
		sources.mqtt().ensureReady();
		stream("mqttSender", "trigger --payload='" + data + "'" + XD_DELIMITER + sinks.mqtt());
		assertValid(data, sinks.file());
		assertReceived(1);

	}

}
