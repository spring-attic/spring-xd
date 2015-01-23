/*
 * Copyright 2014 the original author or authors.
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

import org.apache.commons.lang.ArrayUtils;

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
@Mixin({KafkaZKOptionMixin.class, KafkaConsumerOptionsMixin.class})
public class KafkaSourceModuleOptionsMetadata implements ProfileNamesProvider {

	private String topic = ModulePlaceholders.XD_STREAM_NAME;

	private String partitions = "";

	private String initialOffsets = "";

	private OffsetStorageStrategy offsetStorage = OffsetStorageStrategy.inmemory;

	private int streams = 1;

	private String groupId = ModulePlaceholders.XD_STREAM_NAME;

	private String encoding = "UTF8";

	@ModuleOption("kafka topic name")
	public void setTopic(String topic) {
		this.topic = topic;
	}

	public String getTopic() {
		return this.topic;
	}

	public String getPartitions() {
		return partitions;
	}

	@ModuleOption("kafka partitions")
	public void setPartitions(String partitions) {
		this.partitions = partitions;
	}

	public String getInitialOffsets() {
		return initialOffsets;
	}

	@ModuleOption("initial offsets")
	public void setInitialOffsets(String initialOffsets) {
		this.initialOffsets = initialOffsets;
	}

	public OffsetStorageStrategy getOffsetStorage() {
		return offsetStorage;
	}

	@ModuleOption("offset storage strategy")
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

	public enum OffsetStorageStrategy {
		inmemory,
		redis
	}

	@Override
	public String[] profilesToActivate() {
		if (offsetStorage != null) {
			return new String[] {String.format("%s-metadata-store", offsetStorage)};
		} else {
			throw new IllegalStateException("An offset storage strategy must be configured");
		}
 	}
}
