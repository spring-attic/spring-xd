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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.integration.kafka.core.ConnectionFactory;
import org.springframework.integration.kafka.core.Partition;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Is responsible for managing the partition allocation between multiple instances of a Kafka source module
 * deployed in a stream.
 * As a {@link FactoryBean} it will return the partitions that the current module instance should listen to.
 * The partitions for the given topic would be extracted from Kafka unless an explicit partition list
 * is provided.
 * Partitions are allocated evenly based on the number of module instances.
 * Zero-count Kafka source modules are not supported.
 *
 * @author Marius Bogoevici
 * @author Ilayaperumal Gopinathan
 */
public class KafkaPartitionAllocator implements FactoryBean<Partition[]> {

	private static final Logger log = LoggerFactory.getLogger(KafkaPartitionAllocator.class);

	private static final Pattern PARTITION_LIST_VALIDATION_REGEX = Pattern.compile("\\d+([,-]\\d+)*");

	private final List<String> topics;

	private final String partitionList;

	private final ConnectionFactory connectionFactory;

	private final int count;

	private final int sequence;

	private final String moduleName;

	private final String streamName;

	public KafkaPartitionAllocator(ConnectionFactory connectionFactory, String moduleName,
			String streamName, String topics, String partitionList, int sequence, int count) {
		Assert.notNull(connectionFactory, "cannot be null");
		Assert.hasText(moduleName, "cannot be empty");
		Assert.hasText(streamName, "cannot be empty");
		Assert.hasText(topics, "cannot be empty");
		Assert.isTrue(sequence > 0, " must be a positive number. 0-count kafka sources are not currently supported");
		Assert.isTrue(count > 0, " must be a positive number. 0-count kafka sources are not currently supported");
		Assert.notNull(count > 0, " must be a positive number. 0-count kafka sources are not currently supported");
		this.connectionFactory = connectionFactory;
		this.moduleName = moduleName;
		this.streamName = streamName;
		this.topics = Arrays.asList(topics.split("\\s*,\\s*"));
		this.partitionList = partitionList;
		this.sequence = sequence;
		this.count = count;
	}

	@Override
	public synchronized Partition[] getObject() throws Exception {
		if (log.isDebugEnabled()) {
			log.debug("Module name is " + moduleName);
			log.debug("Stream name is " + streamName);
			log.debug("Cardinality is " + count);
			log.debug("Sequence is " + sequence);
		}
		Map<String, Collection<Partition>> partitionsMapByTopic = new HashMap<String, Collection<Partition>>();
		int maxPartitionCount = 0;
		for (String topic : topics) {
			List<Partition> partitions = new ArrayList<Partition>(connectionFactory.getPartitions(topic));
			Collections.sort(partitions, new Comparator<Partition>() {
				@Override
				public int compare(Partition partition1, Partition partition2) {
					return partition1.getId() - partition2.getId();
				}
			});
			partitionsMapByTopic.put(topic, partitions);
			maxPartitionCount = (partitions.size() > maxPartitionCount) ? partitions.size() : maxPartitionCount;
		}
		Assert.isTrue(maxPartitionCount >= count, "Total module count should not be less than the maximum of " +
				"partitions from the given topics");
		if (topics.size() > 1) {
			Assert.isTrue(!StringUtils.hasText(partitionList), "Explicit partitions list isn't supported for " +
					"multi-topics");
		}
		Map<String, Collection<Partition>> partitionsToListen = StringUtils.hasText(partitionList) ?
				toPartitionsMap(topics.get(0), parseNumberList(partitionList)) : partitionsMapByTopic;
		Collection<Partition> partitionsToReturn = new ArrayList<Partition>();
		// To evenly distribute the partitions, assign the group of deterministic partitions to the given moduleInstance
		for (Collection<Partition> partitions : partitionsToListen.values()) {
			Partition[] partitionsArray = partitions.toArray(new Partition[partitions.size()]);
			for (int i = (sequence - 1); i < partitions.size(); i = i + count) {
				partitionsToReturn.add(partitionsArray[i]);
			}
		}
		return partitionsToReturn.toArray(new Partition[partitionsToReturn.size()]);
	}

	/**
	 * @param topic the topic name which is the key for the map
	 * @param partitionIds the partition Ids to map
	 * @return the map of {@ref Partition} collections for the given topic.
	 */
	private Map<String, Collection<Partition>> toPartitionsMap(String topic, Iterable<Integer> partitionIds) {
		List<Partition> partitions = new ArrayList<Partition>();
		for (Integer partitionId : partitionIds) {
			partitions.add(new Partition(topic, partitionId));
		}
		Map<String, Collection<Partition>> partitionsMap = new HashMap<String, Collection<Partition>>();
		partitionsMap.put(topic, partitions);
		return partitionsMap;
	}

	/**
	 * Expects a String containing a list of numbers or ranges, e.g. {@code "1-10"}, {@code "1,3,5"},
	 * {@code "1,5,10-20,26,100-110,145"}. One-sized ranges or ranges where the start is after the end
	 * are not permitted.
	 * Returns an array of Integers containing the actual numbers.
	 *
	 * @param numberList a string containing numbers, or ranges
	 * @return the list of integers
	 * @throws IllegalArgumentException if the format of the list is incorrect
	 */
	public static Iterable<Integer> parseNumberList(String numberList) throws IllegalArgumentException {
		Assert.hasText(numberList, "must contain a list of values");
		Assert.isTrue(PARTITION_LIST_VALIDATION_REGEX.matcher(numberList).matches(), "is not a list of numbers or ranges");
		Set<Integer> numbers = new TreeSet<Integer>();
		String[] numbersOrRanges = numberList.split(",");
		for (String numberOrRange : numbersOrRanges) {
			if (numberOrRange.contains("-")) {
				String[] split = numberOrRange.split("-");
				Integer start = Integer.parseInt(split[0]);
				Integer end = Integer.parseInt(split[1]);
				if (start >= end) {
					throw new IllegalArgumentException(String.format("A range contains a start which is after the end: %d-%d",
							start, end));
				}
				for (int i = start; i <= end; i++) {
					numbers.add(i);
				}
			}
			else {
				numbers.add(Integer.parseInt(numberOrRange));
			}
		}
		return Collections.unmodifiableSet(numbers);
	}

	@Override
	public Class<?> getObjectType() {
		return Partition[].class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}
}
