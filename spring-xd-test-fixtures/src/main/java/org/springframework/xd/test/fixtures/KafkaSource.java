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
package org.springframework.xd.test.fixtures;

import org.springframework.util.StringUtils;
import reactor.util.Assert;

/**
 * A test fixture that represents the kafka source
 *
 * @author Glenn Renfro
 */

public class KafkaSource extends AbstractModuleFixture<KafkaSource> {

	public static final String DEFAULT_ZK_CLIENT = "localhost:2181";
	public static final String DEFAULT_TOPIC = "mytopic";
	public static final String DEFAULT_OUTPUT_TYPE = "text/plain";
	private String zkConnect;
	private String topic = DEFAULT_TOPIC;
	private String outputType = DEFAULT_OUTPUT_TYPE;

	/**
	 * Initializes a KafkaSource fixture;
	 *
	 * @param zkConnect The zookeeper connection string.
	 */
	public KafkaSource(String zkConnect) {
		Assert.hasText(zkConnect, "zkConnect must not be empty nor null");
		this.zkConnect = zkConnect;
	}

	/**
	 * Returns an instance of the KafkaSource using defaults.
	 *
	 * @return instance of the KafkaSource
	 */
	public static KafkaSource withDefaults() {
		return new KafkaSource(DEFAULT_ZK_CLIENT);
	}

	@Override
	protected String toDSL() {
		return String.format("kafka --zkconnect=%s --topic=%s  --outputType=%s", zkConnect, topic, outputType);
	}

	/**
	 * sets the topic for the kafka source
	 *
	 * @param topic the topic that data will be posted.
	 * @return instance of the KafkaSource
	 */
	public KafkaSource topic(String topic) {
		Assert.hasText(topic, "topic must not be empty nor null");
		this.topic = topic;
		return this;
	}

	/**
	 * set the zkConnect for the kafka source
	 *
	 * @param zkConnect the zookeeper connection string to be used
	 * @return instance of the kafka source
	 */
	public KafkaSource zkConnect(String zkConnect) {
		Assert.hasText(zkConnect, "zkConnect must not be empty nor null");
		this.zkConnect = zkConnect;
		return this;
	}

	/**
	 * set the outputType for the kafka source
	 *
	 * @param outputType the output type to be used.
	 * @return instance of the kafka source
	 */
	public KafkaSource outputType(String outputType) {
		Assert.hasText(outputType, "outputType must not be empty nor null");
		this.outputType = outputType;
		return this;
	}

	/**
	 * Ensure that the zookeeper  socket is available by polling it for up to 2 seconds
	 *
	 * @return instance of the kafka source
	 * @throws IllegalStateException if can not connect in 2 seconds.
	 */
	public KafkaSource ensureReady() {
		String[] addressArray = StringUtils.commaDelimitedListToStringArray(zkConnect);
		for (String address : addressArray) {
			String[] zkAddressArray = StringUtils.delimitedListToStringArray(address, ":");
			Assert.isTrue(zkAddressArray.length == 2,
					"zkConnect data was not properly formatted");
			String host = zkAddressArray[0];
			int port = Integer.valueOf(zkAddressArray[1]);
			AvailableSocketPorts.ensureReady(this, host, port, 2000);
		}
		return this;
	}

}
