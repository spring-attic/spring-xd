/*
 * Copyright 2014-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.test.kafka;


import java.util.Properties;

import kafka.server.KafkaConfig;
import kafka.server.KafkaServer;
import kafka.utils.SystemTime$;
import kafka.utils.TestUtils;
import kafka.utils.TestZKUtils;
import kafka.utils.Utils;
import kafka.utils.ZKStringSerializer$;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.exception.ZkInterruptedException;
import org.junit.Rule;

import org.springframework.xd.test.AbstractExternalResourceTestSupport;

/**
 * JUnit {@link Rule} that starts an embedded Kafka server (with an associated Zookeeper)
 *
 * @author Ilayaperumal Gopinathan
 * @author Marius Bogoevici
 * @since 1.1
 */
public class KafkaTestSupport extends AbstractExternalResourceTestSupport<String> {

	private ZkClient zkClient;

	private EmbeddedZookeeper zookeeper;

	private KafkaServer kafkaServer;

	private Properties brokerConfig = TestUtils.createBrokerConfig(0, TestUtils.choosePort());

	public KafkaTestSupport() {
		super("KAFKA");
	}

	public KafkaTestSupport(String ... properties) {
		super("KAFKA");
		if (properties.length % 2 != 0) {
			throw new IllegalArgumentException("A list of key value pairs must be provided");
		}
		for (int i = 0; i < properties.length / 2; i++) {
			this.brokerConfig.setProperty(properties[i], properties[i+1]);
		}
	}

	public void setBrokerConfig(Properties brokerConfig) {
		this.brokerConfig = brokerConfig;
	}

	public String getZkconnectstring() {
		return zookeeper.getConnectString();
	}

	public ZkClient getZkClient() {
		return this.zkClient;
	}

	public EmbeddedZookeeper getZookeeper() {
		return zookeeper;
	}

	public KafkaServer getKafkaServer() {
		return kafkaServer;
	}

	public String getBrokerAddress() {
		return getKafkaServer().config().hostName() + ":" + getKafkaServer().config().port();
	}

	@Override
	protected void obtainResource() throws Exception {
		try {
			zookeeper = new EmbeddedZookeeper(TestZKUtils.zookeeperConnect());
		}
		catch (Exception e) {
			throw new RuntimeException("Issues creating the ZK server", e);
		}
		try {
			int zkConnectionTimeout = 6000;
			int zkSessionTimeout = 6000;
			zkClient = new ZkClient(getZkconnectstring(), zkSessionTimeout, zkConnectionTimeout, ZKStringSerializer$.MODULE$);
		}
		catch (Exception e) {
			zookeeper.shutdown();
			throw new RuntimeException("Issues creating the ZK client", e);
		}
		try {
			Properties brokerConfigProperties = brokerConfig;
			kafkaServer = TestUtils.createServer(new KafkaConfig(brokerConfigProperties), SystemTime$.MODULE$);
		}
		catch (Exception e) {
			zookeeper.shutdown();
			zkClient.close();
			throw new RuntimeException("Issues creating the Kafka server", e);
		}
	}

	@Override
	protected void cleanupResource() throws Exception {
		try {
			kafkaServer.shutdown();
		}
		catch (Exception e) {
			// ignore errors on shutdown
		}
		try {
			Utils.rm(kafkaServer.config().logDirs());
		}
		catch (Exception e) {
			// ignore errors on shutdown
		}
		try {
			zkClient.close();
		}
		catch (ZkInterruptedException e) {
			// ignore errors on shutdown
		}
		try {
			zookeeper.shutdown();
		}
		catch (Exception e) {
			// ignore errors on shutdown
		}
	}

}
