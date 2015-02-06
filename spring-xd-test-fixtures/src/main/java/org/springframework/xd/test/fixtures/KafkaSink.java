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

import org.springframework.util.Assert;
import org.springframework.xd.test.fixtures.util.KafkaUtils;


/**
 * A test fixture that represents the kafka sink
 *
 * @author Glenn Renfro
 */

public class KafkaSink extends AbstractModuleFixture<KafkaSink> {

	public static final String DEFAULT_BROKER_LIST = "localhost:9092";
	public static final String DEFAULT_TOPIC = "mytopic";
	private String zkConnect;
	private String topic = DEFAULT_TOPIC;

	/**
	 * Initializes a Sink fixture;
	 *
	 * @param zkConnect The list of brokers to connect.
	 */
	public KafkaSink(String zkConnect) {
		Assert.hasText(zkConnect, "brokerList must not be empty nor null");
		this.zkConnect = zkConnect;
	}

	/**
	 * Returns an instance of the KafkaSink using defaults.
	 *
	 * @return instance of the KafkaSink
	 */
	public static KafkaSink withDefaults() {
		return new KafkaSink(DEFAULT_BROKER_LIST);
	}

	@Override
	protected String toDSL() {
		return String.format("kafka --brokerList=%s --topic=%s", zkConnect, topic);
	}

	/**
	 * Set the brokerList for the sink
	 *
	 * @param zkConnect A list of brokers that the sink can connect
	 * @return instance of Kafka sink
	 */
	public KafkaSink brokerList(String zkConnect) {
		Assert.hasText(zkConnect, "brokerList must not be empty nor null");
		this.zkConnect = zkConnect;
		return this;
	}

	/**
	 * Set the topic that the sink will publish to
	 *
	 * @param topic the topic the sink will publish
	 * @return instance of the kafka sink
	 */
	public KafkaSink topic(String topic) {
		Assert.hasText(topic, "topic must not be empty nor null");
		this.topic = topic;
		return this;
	}

	/**
	 * Ensure that the kafka broker socket is available by polling it for up to 2 seconds
	 *
	 * @return instance of the KafkaSink
	 * @throws IllegalStateException if can not connect in 2 seconds.
	 */
	public KafkaSink ensureReady() {
		KafkaUtils.ensureReady(this.toString(), this.zkConnect, this.topic);
		return this;
	}

}
