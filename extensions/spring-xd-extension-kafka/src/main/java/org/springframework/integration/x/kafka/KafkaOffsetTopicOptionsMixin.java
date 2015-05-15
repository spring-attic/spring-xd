/*
 * Copyright 2015 the original author or authors.
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

import org.springframework.xd.module.options.spi.ModuleOption;

/**
 * @author Marius Bogoevici
 */
public class KafkaOffsetTopicOptionsMixin {

	private int kafkaOffsetTopicSegmentSize = 250 * 1024 * 1024;

	private String kafkaOffsetTopicName = "${xd.stream.name}-${xd.module.name}-offsets";

	private int kafkaOffsetTopicRetentionTime = 60000;

	private int kafkaOffsetTopicRequiredAcks = 1;

	private int kafkaOffsetTopicMaxSize = 1024*1024;

	private int kafkaOffsetTopicBatchBytes = 200;

	private int kafkaOffsetTopicBatchTime = 1000;

	public int getKafkaOffsetTopicSegmentSize() {
		return kafkaOffsetTopicSegmentSize;
	}

	@ModuleOption(value = "segment size of the offset topic, if Kafka offset strategy is chosen")
	public void setKafkaOffsetTopicSegmentSize(int kafkaOffsetTopicSegmentSize) {
		this.kafkaOffsetTopicSegmentSize = kafkaOffsetTopicSegmentSize;
	}

	public String getKafkaOffsetTopicName() {
		return kafkaOffsetTopicName;
	}

	@ModuleOption(value = "name of the offset topic, if Kafka offset strategy is chosen")
	public void setKafkaOffsetTopicName(String kafkaOffsetTopicName) {
		this.kafkaOffsetTopicName = kafkaOffsetTopicName;
	}

	public int getKafkaOffsetTopicRetentionTime() {
		return kafkaOffsetTopicRetentionTime;
	}

	@ModuleOption(value = "retention time for dead records (tombstones), if Kafka offset strategy is chosen", hidden = true)
	public void setKafkaOffsetTopicRetentionTime(int kafkaOffsetTopicRetentionTime) {
		this.kafkaOffsetTopicRetentionTime = kafkaOffsetTopicRetentionTime;
	}

	public int getKafkaOffsetTopicRequiredAcks() {
		return kafkaOffsetTopicRequiredAcks;
	}

	@ModuleOption(value = "required acks for writing to the Kafka offset topic, if Kafka offset strategy is chosen", hidden = true)
	public void setKafkaOffsetTopicRequiredAcks(int kafkaOffsetTopicRequiredAcks) {
		this.kafkaOffsetTopicRequiredAcks = kafkaOffsetTopicRequiredAcks;
	}

	public int getKafkaOffsetTopicMaxSize() {
		return kafkaOffsetTopicMaxSize;
	}

	@ModuleOption(value = "maximum size of reads from offset topic, if Kafka offset strategy is chosen", hidden = true)
	public void setKafkaOffsetTopicMaxSize(int kafkaOffsetTopicMaxSize) {
		this.kafkaOffsetTopicMaxSize = kafkaOffsetTopicMaxSize;
	}

	public int getKafkaOffsetTopicBatchBytes() {
		return kafkaOffsetTopicBatchBytes;
	}

	@ModuleOption(value = "maximum batched bytes for writes to offset topic, if Kafka offset strategy is chosen", hidden = true)
	public void setKafkaOffsetTopicBatchBytes(int kafkaOffsetTopicBatchBytes) {
		this.kafkaOffsetTopicBatchBytes = kafkaOffsetTopicBatchBytes;
	}

	public int getKafkaOffsetTopicBatchTime() {
		return kafkaOffsetTopicBatchTime;
	}

	@ModuleOption(value = "maximum time for batching writes to offset topic, if Kafka offset strategy is chosen", hidden = true)
	public void setKafkaOffsetTopicBatchTime(int kafkaOffsetTopicBatchTime) {
		this.kafkaOffsetTopicBatchTime = kafkaOffsetTopicBatchTime;
	}

}
