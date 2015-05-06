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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.integration.kafka.core.Partition;
import org.springframework.integration.kafka.listener.OffsetManager;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Parses the list of initial offsets and creates a map to initialize the {@link OffsetManager}
 *
 * @author Marius Bogoevici
 * @author Ilayaperumal Gopinathan
 */
public class InitialOffsetsFactoryBean implements FactoryBean<Map<Partition, Long>> {

	// Matches expressions like 0@20,1@50 etc.
	public static final Pattern VALIDATION_PATTERN = Pattern.compile("(\\d+@\\d+)[,(\\d+@\\d+)]*");

	private final List<String> topics;

	private String initialOffsetList;

	public InitialOffsetsFactoryBean(String topics, String initialOffsetList) {
		this.topics = Arrays.asList(topics.split("\\s*,\\s*"));
		Assert.isTrue(!topics.isEmpty(), "Topic names must be provided");
		this.initialOffsetList = initialOffsetList;
	}

	@Override
	public Map<Partition, Long> getObject() throws Exception {
		//TODO: support initial offsets for multiple topics' partitions
		return (StringUtils.hasText(initialOffsetList) && topics.size() == 1) ?
				parseOffsetList(topics.get(0), initialOffsetList) : Collections.<Partition, Long>emptyMap();
	}

	@Override
	public Class<?> getObjectType() {
		return Map.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	private static Map<Partition, Long> parseOffsetList(String topic, String offsetList) throws IllegalArgumentException {
		Assert.hasText(offsetList, "must contain a list of values");
		Assert.isTrue(VALIDATION_PATTERN.matcher(offsetList).matches(), "must be in the form 0@20");
		Map<Partition, Long> partitionNumbers = new HashMap<Partition, Long>();
		String[] partitionOffsetPairs = offsetList.split(",");
		for (String partitionOffsetPair : partitionOffsetPairs) {
			String[] split = partitionOffsetPair.split("@");
			partitionNumbers.put(new Partition(topic, Integer.parseInt(split[0])), Long.parseLong(split[1]));
		}
		return partitionNumbers;
	}

}
