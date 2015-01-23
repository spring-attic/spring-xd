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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import kafka.api.OffsetRequest;

import org.springframework.integration.kafka.core.ConnectionFactory;
import org.springframework.integration.kafka.core.Partition;
import org.springframework.integration.kafka.listener.MetadataStoreOffsetManager;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Spring Integration Kafka {@link MetadataStoreOffsetManager} with additional features such
 * as the ability to delete offsets, and recognizing Kafka consumer code for "earliest time"
 * and "latest time" ("smallest" and "largest" respectively).
 *
 * @author Marius Bogoevici
 */
public class SpringXdOffsetManager extends MetadataStoreOffsetManager implements DeletableOffsetManager {

	// Matches expressions like 0@20,1@50 etc.
	public static final Pattern VALIDATION_PATTERN = Pattern.compile("(\\d+@\\d+)[,(\\d+@\\d+)]*");

	public SpringXdOffsetManager(ConnectionFactory connectionFactory) {
		super(connectionFactory);
	}

	/**
	 * Creates a {@link MetadataStoreOffsetManager} with
	 *
	 * @param connectionFactory
	 * @param initialOffsets
	 */
	@SuppressWarnings("unchecked")
	public SpringXdOffsetManager(ConnectionFactory connectionFactory, String topic, String initialOffsets) {
		super(connectionFactory,
				StringUtils.hasText(initialOffsets) ? parseOffsetList(topic, initialOffsets) : Collections.EMPTY_MAP);
	}

	@Override
	public void deleteOffset(Partition partition) {
		getMetadataStore().remove(partition.getTopic() + ":" + partition.getId() + ":" + getConsumerId());
	}

	public void setAutoOffsetReset(AutoOffsetResetStrategy autoOffsetResetStrategy) {
		this.setReferenceTimestamp(autoOffsetResetStrategy.code());
	}

	private static Map<Partition, Long> parseOffsetList(String topic, String offsetList) throws IllegalArgumentException {
		Assert.hasText(offsetList, "must contain a list of values");
		Assert.isTrue(VALIDATION_PATTERN.matcher(offsetList).matches(), "must be in the form 0@20");
		Map<Partition, Long> partitionNumbers = new HashMap<Partition, Long>();
		String[] partitionOffsetPairs = offsetList.split(",");
		for (String partitionOffsetPair : partitionOffsetPairs) {
			String[] split = partitionOffsetPair.split("@");
			partitionNumbers.put(new Partition(topic,Integer.parseInt(split[0])), Long.parseLong(split[1]));
		}
		return partitionNumbers;
	}

	public static enum AutoOffsetResetStrategy {

		smallest(OffsetRequest.EarliestTime()),
		largest(OffsetRequest.LatestTime());

		private long code;

		AutoOffsetResetStrategy(long code) {
			this.code = code;
		}

		public long code() {
			return code;
		}
	}

}
