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

import org.springframework.data.redis.core.RedisOperations;
import org.springframework.xd.dirt.stream.TapDefinition;
import org.springframework.xd.dirt.stream.TapDefinitionRepository;
import org.springframework.xd.store.AbstractRedisRepository;

/**
 * An implementation of {@link TapDefinitionRepository} that persists @{link TapDefinition} in Redis.
 * 
 * @author Eric Bottard
 * @author David Turanski
 */
public class RedisTapDefinitionRepository extends AbstractRedisRepository<TapDefinition, String> implements
		TapDefinitionRepository {

	public RedisTapDefinitionRepository(RedisOperations<String, String> redisOperations) {
		super("tap.definitions.", redisOperations);
	}

	@Override
	protected TapDefinition deserialize(String v) {
		String[] parts = v.split("\n");
		return new TapDefinition(parts[0], parts[1], parts[2]);
	}

	@Override
	protected String serialize(TapDefinition entity) {
		return entity.getName() + "\n" + entity.getStreamName() + "\n" + entity.getDefinition();
	}

	@Override
	protected String keyFor(TapDefinition entity) {
		return entity.getName();
	}

	@Override
	protected String serializeId(String id) {
		return id;
	}
}
