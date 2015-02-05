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

import static org.apache.curator.framework.imps.CuratorFrameworkState.STARTED;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.utils.EnsurePath;
import org.apache.zookeeper.KeeperException;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.integration.kafka.core.BrokerAddress;
import org.springframework.integration.kafka.core.ConnectionFactory;
import org.springframework.integration.kafka.core.Partition;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Is responsible for managing the partition allocation between multiple instances of a Kafka source module
 * deployed in a stream.
 * As a {@link FactoryBean} it will return the partitions that the current module instance should listen to.
 * The list of partitions is stored in ZooKeeper, and is created when the module is started for the first time.
 * If multiple module instances are started at the same time, they synchronize via ZooKeeper, and only the first
 * will end up creating the module list, while the other instances will read it.
 * Restarted modules will read the list of partitions that corresponds to their own sequence. Zero-count Kafka source
 * modules are not supported.
 * As an {@link ApplicationListener}, it checks whether the current Stream state cleans up the partition table in
 * ZooKeeper when the stream is undeploying by validating that against the stream data path in ZooKeeper.
 *
 * @author Marius Bogoevici
 */
public class KafkaPartitionAllocator implements InitializingBean, FactoryBean<Partition[]>,
		ApplicationListener<ContextClosedEvent> {

	private static final Log log = LogFactory.getLog(KafkaPartitionAllocator.class);

	private static final Pattern PARTITION_LIST_VALIDATION_REGEX =
			Pattern.compile("\\d+([,-]\\d+)*");

	public static final String STREAM_STATUS_UNDEPLOYED = "undeployed";

	public static final String STREAM_STATUS_UNDEPLOYING = "undeploying";

	public static final String STREAM_PATH_PATTERN = "deployments/streams/%s/status";

	private final String topic;

	private final String partitionList;

	private final ConnectionFactory connectionFactory;

	private final int count;

	private final int sequence;

	private final CuratorFramework client;

	private final DeletableOffsetManager offsetManager;

	private final String moduleName;

	private final String streamName;

	private final ObjectMapper objectMapper = new ObjectMapper();

	private final String partitionDataPath;

	private volatile Partition[] partitions;

	private InterProcessMutex partitionDataMutex;

	public KafkaPartitionAllocator(CuratorFramework client, ConnectionFactory connectionFactory, String moduleName,
			String streamName, String topic, String partitionList, int sequence, int count,
			DeletableOffsetManager offsetMetadataStore) {
		Assert.notNull(connectionFactory, "cannot be null");
		Assert.hasText(moduleName, "cannot be empty");
		Assert.hasText(streamName, "cannot be empty");
		Assert.hasText(topic, "cannot be empty");
		Assert.isTrue(sequence > 0, " must be a positive number. 0-count kafka sources are not currently supported");
		Assert.isTrue(count > 0, " must be a positive number. 0-count kafka sources are not currently supported");
		Assert.notNull(count > 0, " must be a positive number. 0-count kafka sources are not currently supported");
		this.client = client;
		this.connectionFactory = connectionFactory;
		this.moduleName = moduleName;
		this.streamName = streamName;
		this.topic = topic;
		this.partitionList = partitionList;
		this.sequence = sequence;
		this.count = count;
		this.offsetManager = offsetMetadataStore;
		this.partitionDataPath = String.format("/sources/%s/%s", this.streamName, this.moduleName);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (STARTED.equals(this.client.getState())) {
			EnsurePath ensurePath = new EnsurePath(partitionDataPath);
			ensurePath.ensure(client.getZookeeperClient());
			this.partitionDataMutex = new InterProcessMutex(client, partitionDataPath);//NOSONAR
		}
		else {
			throw new BeanInitializationException("Cannot connect to ZooKeeper, client state is " + client.getState());
		}
	}

	@Override
	public synchronized Partition[] getObject() throws Exception {
		if (log.isDebugEnabled()) {
			log.debug("Module name is " + moduleName);
			log.debug("Stream name is " + streamName);
			log.debug("Cardinality is " + count);
			log.debug("Sequence is " + sequence);
			log.debug("Client is " + client);
		}
		if (partitions == null) {
			if (STARTED.equals(client.getState())) {
				try {
					partitionDataMutex.acquire();
					byte[] partitionData = client.getData().forPath(partitionDataPath);
					if (partitionData == null || partitionData.length == 0) {
						Collection<Partition> existingPartitions = connectionFactory.getPartitions(topic);
						Collection<Partition> listenedPartitions = !StringUtils.hasText(partitionList) ?
								existingPartitions : Arrays.asList(toPartitions(parseNumberList(partitionList)));
						if (existingPartitions != listenedPartitions && !existingPartitions.containsAll(listenedPartitions)) {
							Collection<Partition> partitionsNotFound = new ArrayList<Partition>(listenedPartitions);
							partitionsNotFound.removeAll(existingPartitions);
							throw new BeanInitializationException("Configuration contains partitions that do not exist on the topic" +
									" or have unavailable leaders: " + StringUtils.collectionToCommaDelimitedString(partitionsNotFound));
						}
						final Map<Partition, BrokerAddress> leaders = connectionFactory.getLeaders(listenedPartitions);
						ArrayList<Partition> sortedPartitions = new ArrayList<Partition>(listenedPartitions);
						Collections.sort(sortedPartitions, new Comparator<Partition>() {
							@Override
							public int compare(Partition o1, Partition o2) {
								int i = leaders.get(o1).toString().compareTo(leaders.get(o2).toString());
								if (i != 0) {
									return i;
								}
								else return o1.getId() - o2.getId();
							}
						});
						if (log.isDebugEnabled()) {
							log.debug("Partitions: " + StringUtils.collectionToCommaDelimitedString(sortedPartitions));
						}
						// calculate the minimum size of a partition group.
						int minimumSize = sortedPartitions.size() / count;
						int remainder = sortedPartitions.size() % count;
						List<List<Integer>> partitionGroups = new ArrayList<List<Integer>>();
						int cursor = 0;
						for (int i = 0; i < count; i++) {
							// first partitions will get an extra element
							int partitionGroupSize = i < remainder ? minimumSize + 1 : minimumSize;
							ArrayList<Integer> partitionGroup = new ArrayList<Integer>();
							for (int j = 0; j < partitionGroupSize; j++) {
								partitionGroup.add(sortedPartitions.get(cursor++).getId());
							}
							if (log.isDebugEnabled()) {
								log.debug("Partitions for " + (i + 1) + " : " + StringUtils.collectionToCommaDelimitedString(partitionGroup));
							}
							partitionGroups.add(partitionGroup);
						}
						byte[] dataAsBytes = objectMapper.writer().writeValueAsBytes(partitionGroups);
						if (log.isDebugEnabled()) {
							log.debug(new String(dataAsBytes));
						}
						client.setData().forPath(partitionDataPath, dataAsBytes);
						// the partition mapping is stored 0-based but sequence/count are 1-based
						if (log.isDebugEnabled()) {
							log.debug("Listening to: " + StringUtils.collectionToCommaDelimitedString(partitionGroups.get(sequence - 1)));
						}
						partitions = toPartitions(partitionGroups.get(sequence - 1));
					}
					else {
						if (log.isDebugEnabled()) {
							log.debug(new String(partitionData));
						}
						@SuppressWarnings("unchecked")
						List<List<Integer>> partitionGroups = objectMapper.reader(List.class).readValue(partitionData);
						// the partition mapping is stored 0-based but sequence/count are 1-based
						if (log.isDebugEnabled()) {
							log.debug("Listening to: " + StringUtils.collectionToCommaDelimitedString(partitionGroups.get(sequence - 1)));
						}
						partitions = toPartitions(partitionGroups.get(sequence - 1));
					}
					return partitions;
				}
				finally {
					if (partitionDataMutex.isAcquiredInThisProcess()) {
						partitionDataMutex.release();
					}
				}
			}
			else {
				throw new BeanInitializationException("Cannot connect to ZooKeeper, client state is " + client.getState());
			}
		}
		else {
			return partitions;
		}
	}

	private String getDataPath() {
		return String.format("/sources/%s/%s", streamName, moduleName);
	}

	private Partition[] toPartitions(Iterable<Integer> partitionIds) {
		List<Partition> partitions = new ArrayList<Partition>();
		for (Integer partitionId : partitionIds) {
			partitions.add(new Partition(topic, partitionId));
		}
		return partitions.toArray(new Partition[partitions.size()]);
	}

	@Override
	public Class<?> getObjectType() {
		return Partition[].class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public void onApplicationEvent(ContextClosedEvent event) {
		CuratorFrameworkState state = client.getState();
		if (state.equals(STARTED)) {
			try {
				byte[] streamStatusData;
				try {
					streamStatusData = client.getData().forPath(String.format(STREAM_PATH_PATTERN, streamName));
				}
				catch (KeeperException.NoNodeException e) {
					// we ignore this error - the stream path does not exist, so it may have been removed already
					// we'll behave as if we have received no data
					streamStatusData = null;
				}
				// use the stored values in Zookeeper directoy, so as to we do not have a dependency on spring-xd-dirt
				String deploymentStatus;
				if (streamStatusData == null || streamStatusData.length == 0) {
					deploymentStatus = STREAM_STATUS_UNDEPLOYED;
				}
				else {
					Map<String, String> statusDataAsMap = objectMapper.reader(Map.class).readValue(streamStatusData);
					deploymentStatus = statusDataAsMap.get("state");
				}
				if (STREAM_STATUS_UNDEPLOYED.equals(deploymentStatus)
						|| STREAM_STATUS_UNDEPLOYING.equals(deploymentStatus)) {
					// remove partitioning data from Kafka
					if (client.checkExists().forPath(getDataPath()) != null) {
						try {
							client.delete().deletingChildrenIfNeeded().forPath(getDataPath());
						}
						catch (KeeperException.NoNodeException e) {
							if (KeeperException.Code.NONODE.equals(e.code())) {
								// ignore, most likely someone else has deleted the path already
							}
							else {
								// this actually was an exception : we cannot recover, but cannot stop either
								log.error("Irrecoverable error while trying to clean partitions table:", e);
							}
						}
					}
					// also, remove offsets
					for (Partition partition : partitions) {
						if (log.isDebugEnabled()) {
							log.debug("Deleting offsets for " + partition.toString());
						}
						offsetManager.deleteOffset(partition);
						try {
							offsetManager.flush();
						}
						catch (IOException e) {
							log.error("Error while trying to flush offsets: " + e);
						}
					}

				}
			}
			catch (Exception e) {
				log.error(e);
			}
		}
		else {
			log.warn("Could not check the stream state and perform cleanup, client state was " + state);
		}
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
}
