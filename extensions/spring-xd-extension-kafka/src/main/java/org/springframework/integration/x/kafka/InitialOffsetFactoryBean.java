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

import com.gs.collections.api.block.function.Function2;
import com.gs.collections.api.tuple.Pair;
import com.gs.collections.impl.tuple.Tuples;
import com.gs.collections.impl.utility.MapIterate;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.integration.kafka.core.Partition;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;


/**
 * @author Marius Bogoevici
 */
public class InitialOffsetFactoryBean implements FactoryBean<Map<Partition, Long>> {

	private Function2<Integer, Long, Pair<Partition, Long>> convertIdsToTopicsFunction =
			new Function2<Integer, Long, Pair<Partition, Long>>() {
				@Override
				public Pair<Partition, Long> value(Integer key, Long value) {
					return Tuples.pair(new Partition(topic, key), value);
				}
			};

	private final String topic;

	private String partitionAndOffsetList;


	public InitialOffsetFactoryBean(String topic, String partitionAndOffsetList) {
		this.topic = topic;
		this.partitionAndOffsetList = partitionAndOffsetList;
	}


	@Override
	public Map<Partition, Long> getObject() throws Exception {
		if (StringUtils.hasText(partitionAndOffsetList)) {
			Map<Integer, Long> map = parseOffsetList(partitionAndOffsetList);
			return MapIterate.collect(map, convertIdsToTopicsFunction);
		}
		else {
			return Collections.emptyMap();
		}
	}

	@Override
	public Class<?> getObjectType() {
		return Map.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	private Map<Integer, Long> parseOffsetList(String offsetList) throws IllegalArgumentException {
		Assert.hasText(offsetList, "must contain a list of values");
		// TODO: regex validate
		if (offsetList.contains("@")) {
			Map<Integer, Long> partitionNumbers = new HashMap<Integer, Long>();
			String[] partitionOffsetPairs = offsetList.split(",");
			for (String partitionOffsetPair : partitionOffsetPairs) {
				String[] split = partitionOffsetPair.split("@");
				partitionNumbers.put(Integer.parseInt(split[0]), Long.parseLong(split[1]));
			}
			return partitionNumbers;
		}
		else {
			return Collections.singletonMap(-1, Long.parseLong(offsetList));
		}
	}
}
