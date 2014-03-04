package org.springframework.xd.test.mqtt;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.xd.test.AbstractExternalResourceTestSupport;

/**
 * @author Luke Taylor
 */
public class MqttTestSupport extends AbstractExternalResourceTestSupport<MqttClient> {

	public MqttTestSupport() {
		super("MQTT");
	}

	@Override
	protected void obtainResource() throws Exception {
		resource = new MqttClient("tcp://localhost:1883", "xd-test-" + System.currentTimeMillis(), new MemoryPersistence());
		resource.connect();
	}

	@Override
	protected void cleanupResource() throws Exception {
		resource.close();
	}

}
