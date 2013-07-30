/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.xd.dirt.stream.redis;

import org.springframework.data.redis.core.RedisOperations;
import org.springframework.xd.dirt.stream.BaseInstance;
import org.springframework.xd.store.AbstractRedisRepository;

/**
 * Helper class for repositories persisting {@link BaseInstance}s, acknowledging the fact that the instance can use the
 * definition name as a key.
 * 
 * @author Eric Bottard
 */
public abstract class AbstractRedisInstanceRepository<I extends BaseInstance<?>> extends
		AbstractRedisRepository<I, String> {

	public AbstractRedisInstanceRepository(String repoPrefix, RedisOperations<String, String> redisOperations) {
		super(repoPrefix, redisOperations);
	}

	@Override
	protected final String keyFor(I entity) {
		return entity.getDefinition().getName();
	}

	@Override
	protected final String serializeId(String id) {
		return id;
	}

	@Override
	protected final String deserializeId(String string) {
		return string;
	}

}
