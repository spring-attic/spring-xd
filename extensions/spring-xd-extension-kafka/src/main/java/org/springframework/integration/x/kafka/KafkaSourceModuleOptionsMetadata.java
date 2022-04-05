/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.x.kafka;

import java.util.Arrays;

import javax.validation.constraints.AssertTrue;

import org.springframework.util.StringUtils;
import org.springframework.xd.module.options.spi.Mixin;
import org.springframework.xd.module.options.spi.ModuleOption;
import org.springframework.xd.module.options.spi.ModulePlaceholders;
import org.springframework.xd.module.options.spi.ProfileNamesProvider;


/**
 * Module options for Kafka source module.
 *
 * @author Ilayaperumal Gopinathan
 * @author Marius Bogoevici
 */
@Mixin({KafkaZKOptionMixin.class, KafkaConsumerOptionsMixin.class, KafkaOffsetTopicOptionsMixin.class})
public class KafkaSourceModuleOptionsMetadata implements ProfileNamesProvider {

	private String topic = "";

	private String topics = "";

	private String partitions = "";

	private String initialOffsets = "";

	private OffsetStorageStrategy offsetStorage = OffsetStorageStrategy.kafka;

	private int streams = 1;

	private String groupId = ModulePlaceholders.XD_STREAM_NAME;

	private String encoding = "UTF8";

	private int offsetUpdateTimeWindow = 10000;

	private int offsetUpdateCount = 0;

	private int offsetUpdateShutdownTimeout = 2000;

	private int queueSize = 8192;

	@ModuleOption("single topic name")
	public void setTopic(String topic) {
		this.topic = topic;
	}

	public String getTopic() {
		return this.topic;
	}

	@ModuleOption("comma separated kafka topic names")
	public void setTopics(String topics) {
		this.topics = topics;
	}

	public String getTopics() {
		return this.topics;
	}

	public String getPartitions() {
		return partitions;
	}

	@ModuleOption("comma separated list of partition IDs to listen on")
	public void setPartitions(String partitions) {
		this.partitions = partitions;
	}

	public String getInitialOffsets() {
		return initialOffsets;
	}

	@ModuleOption("comma separated list of <partition>@<offset> pairs indicating where the source should" +
			" start consuming from")
	public void setInitialOffsets(String initialOffsets) {
		this.initialOffsets = initialOffsets;
	}

	public OffsetStorageStrategy getOffsetStorage() {
		return offsetStorage;
	}

	@ModuleOption("strategy for persisting offset values")
	public void setOffsetStorage(OffsetStorageStrategy offsetStorage) {
		this.offsetStorage = offsetStorage;
	}

	@ModuleOption("number of streams in the topic")
	public void setStreams(int streams) {
		this.streams = streams;
	}

	public int getStreams() {
		return this.streams;
	}

	@ModuleOption("kafka consumer configuration group id")
	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	public String getGroupId() {
		return this.groupId;
	}

	@ModuleOption("string encoder to translate bytes into string")
	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	public String getEncoding() {
		return this.encoding;
	}

	@ModuleOption("frequency (in milliseconds) with which offsets are persisted " +
			"mutually exclusive with the count-based offset update option (use 0 to disable either)")
	public void setOffsetUpdateTimeWindow(int offsetUpdateTimeWindow) {
		this.offsetUpdateTimeWindow = offsetUpdateTimeWindow;
	}

	public int getOffsetUpdateTimeWindow() {
		return offsetUpdateTimeWindow;
	}

	@ModuleOption("frequency, in number of messages, with which offsets are persisted, per concurrent processor, " +
			"mutually exclusive with the time-based offset update option (use 0 to disable either)")
	public void setOffsetUpdateCount(int offsetUpdateCount) {
		this.offsetUpdateCount = offsetUpdateCount;
	}

	public int getOffsetUpdateCount() {
		return offsetUpdateCount;
	}

	@ModuleOption(value = "timeout for ensuring that all offsets have been written, on shutdown", hidden = true)
	public void setOffsetUpdateShutdownTimeout(int offsetUpdateShutdownTimeout) {
		this.offsetUpdateShutdownTimeout = offsetUpdateShutdownTimeout;
	}

	public int getOffsetUpdateShutdownTimeout() {
		return offsetUpdateShutdownTimeout;
	}

	@ModuleOption(value = "the maximum number of messages held internally and waiting for processing, " +
			"per concurrent handler. Value must be a power of 2")
	public void setQueueSize(int queueSize) {
		this.queueSize = queueSize;
	}

	public int getQueueSize() {
		return queueSize;
	}

	public enum OffsetStorageStrategy {
		inmemory,
		redis,
		kafka,
		kafkaNative
	}

	@AssertTrue(message = "the options topic and topics are mutually exclusive")
	public boolean isTopicOptionValid() {
		boolean isTopicValid = StringUtils.hasText(topic);
		boolean isTopicsValid = StringUtils.hasText(topics);
		boolean isOptionValid =   isTopicValid  ? !isTopicsValid : true;
		if (isOptionValid && (!isTopicsValid && !isTopicValid)) {
			this.topic = ModulePlaceholders.XD_STREAM_NAME;
			this.topics = this.topic;
		}
		else if (isOptionValid && isTopicValid) {
				this.topics = this.topic;
		}
		return isOptionValid;
	}

	@AssertTrue(message = "explicit partitions can only be set when using single topic source")
	public boolean isPartitionsOptionValid() {
		return (Arrays.asList(topics.split("\\s*,\\s*")).size() > 1) ? !StringUtils.hasText(partitions) : true;
	}

	@AssertTrue(message = "initial offsets can only be set when using single topic source")
	public boolean isInitialOffsetsValid() {
		return (Arrays.asList(topics.split("\\s*,\\s*")).size() > 1) ? !StringUtils.hasText(initialOffsets) : true;
	}

	@Override
	public String[] profilesToActivate() {
		if (offsetStorage != null) {
			return new String[] {String.format("%s-offset-manager", offsetStorage)};
		}
		else {
			throw new IllegalStateException("An offset storage strategy must be configured");
		}
	}
}
