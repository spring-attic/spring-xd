/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.dirt.stream.redis;

import java.util.Date;

import org.springframework.data.redis.core.RedisOperations;
import org.springframework.xd.dirt.stream.Stream;
import org.springframework.xd.dirt.stream.StreamDefinition;
import org.springframework.xd.dirt.stream.StreamDefinitionRepository;
import org.springframework.xd.dirt.stream.StreamRepository;

/**
 * Redis implementation of {@link StreamRepository}, uses a {@link RedisStreamDefinitionRepository} in turn.
 * 
 * @author Eric Bottard
 * 
 */
public class RedisStreamRepository extends AbstractRedisInstanceRepository<Stream> implements StreamRepository {

	private final StreamDefinitionRepository streamDefinitionRepository;

	public RedisStreamRepository(RedisOperations<String, String> redisOperations,
			StreamDefinitionRepository streamDefinitionRepository) {
		super("streams", redisOperations);
		this.streamDefinitionRepository = streamDefinitionRepository;
	}

	@Override
	protected Stream deserialize(String redisKey, String v) {
		String[] parts = v.split("\n");
		StreamDefinition def = streamDefinitionRepository.findOne(parts[0]);
		Date startedAt = new Date(Long.parseLong(parts[1]));
		Stream stream = new Stream(def);
		stream.setStartedAt(startedAt);
		return stream;
	}

	@Override
	protected String serialize(Stream entity) {
		// Store def name (which happens to be stream name, and properties)
		return entity.getDefinition().getName() + "\n" + entity.getStartedAt().getTime();
	}

}
