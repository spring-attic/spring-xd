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
import org.springframework.xd.dirt.stream.TapDefinition;
import org.springframework.xd.dirt.stream.TapInstance;
import org.springframework.xd.dirt.stream.TapInstanceRepository;
import org.springframework.xd.store.AbstractRedisRepository;

/**
 * Redis implementation of {@link TapInstanceRepository}, uses a {@link RedisTapDefinitionRepository} in turn.
 *
 * @author Gunnar Hillert
 * @since 1.0
 *
 */
public class RedisTapInstanceRepository extends AbstractRedisRepository<TapInstance, String> implements TapInstanceRepository {

	private final RedisTapDefinitionRepository redisTapDefinitionRepository;

	public RedisTapInstanceRepository(RedisOperations<String, String> redisOperations,
			RedisTapDefinitionRepository redisTapDefinitionRepository) {
		super("taps.", redisOperations);
		this.redisTapDefinitionRepository = redisTapDefinitionRepository;
	}

	@Override
	protected TapInstance deserialize(String v) {
		String[] parts = v.split("\n");
		TapDefinition def = redisTapDefinitionRepository.findOne(parts[0]);
		Date startedAt = new Date(Long.parseLong(parts[1]));
		TapInstance tapInstance = new TapInstance(def);
		tapInstance.setStartedAt(startedAt);
		return tapInstance;
	}

	@Override
	protected String serialize(TapInstance entity) {
		// Store def name (which happens to be stream name, and properties)
		return entity.getDefinition().getName() + "\n" + entity.getStartedAt().getTime();
	}

	@Override
	protected String keyFor(TapInstance entity) {
		return entity.getDefinition().getName();
	}

	@Override
	protected String serializeId(String id) {
		return id;
	}

}
