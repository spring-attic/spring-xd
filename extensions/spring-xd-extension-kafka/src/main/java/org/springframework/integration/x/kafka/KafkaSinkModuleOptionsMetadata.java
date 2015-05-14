/*
 * Copyright 2014-2015 the original author or authors.
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

package org.springframework.integration.x.kafka;

import org.springframework.xd.module.options.spi.Mixin;
import org.springframework.xd.module.options.spi.ModuleOption;
import org.springframework.xd.module.options.spi.ModulePlaceholders;


/**
 * Module options for Kafka sink module.
 *
 * @author Ilayaperumal Gopinathan
 */
@Mixin(KafkaProducerOptionsMixin.class)
public class KafkaSinkModuleOptionsMetadata {

	private String topic = ModulePlaceholders.XD_STREAM_NAME;

	private String brokerList = "localhost:9092";

	@ModuleOption("kafka topic name")
	public void setTopic(String topic) {
		this.topic = topic;
	}

	public String getTopic() {
		return this.topic;
	}

	@ModuleOption("comma separated broker list")
	public void setBrokerList(String brokerList) {
		this.brokerList = brokerList;
	}

	public String getBrokerList() {
		return brokerList;
	}
}
