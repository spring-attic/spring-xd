/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.xd.dirt.module.store;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.util.Assert;
import org.springframework.xd.store.AbstractRedisRepository;


/**
 * Abstract class that has redis hashOperations to operate on {@link RuntimeModuleInfoEntity}s.
 * 
 * @author Ilayaperumal Gopinathan
 */
public abstract class AbstractRedisModuleInfoRepository extends AbstractRedisRepository<RuntimeModuleInfoEntity, String> {

	private static final int CONTAINER_ID_INDEX = 0;

	private static final int GROUP_INDEX = 1;

	private static final int INDEX = 2;

	private static final int PROPERTIES_INDEX = 3;

	private HashOperations<String, String, String> hashOperations;

	public AbstractRedisModuleInfoRepository(String repoPrefix, RedisOperations<String, String> redisOperations) {
		super(repoPrefix, redisOperations);
		hashOperations = redisOperations.opsForHash();
	}

	protected HashOperations<String, String, String> getHashOperations() {
		return this.hashOperations;
	}

	@Override
	protected String serializeId(String id) {
		return id;
	}

	@Override
	protected String deserializeId(String string) {
		return string;
	}

	@Override
	protected String keyFor(RuntimeModuleInfoEntity entity) {
		return entity.getId();
	}

	@Override
	protected RuntimeModuleInfoEntity deserialize(String redisKey, String value) {
		String[] parts = value.split("\n");
		return new RuntimeModuleInfoEntity(parts[CONTAINER_ID_INDEX], parts[GROUP_INDEX], parts[INDEX], parts[PROPERTIES_INDEX]);
	}

	@Override
	protected String serialize(RuntimeModuleInfoEntity entity) {
		return entity.getContainerId() + "\n" + entity.getGroup() + "\n" + entity.getIndex() + "\n"
				+ entity.getProperties();
	}

	@Override
	protected String redisKeyFromId(String id) {
		Assert.notNull(id);
		return repoPrefix + ":" + serializeId(id);
	}

	protected String redisHashKeyFromEntity(RuntimeModuleInfoEntity entity) {
		return entity.getGroup() + ":" + entity.getIndex();
	}

	protected abstract String keyForEntity(RuntimeModuleInfoEntity entity);

	@Override
	public <S extends RuntimeModuleInfoEntity> S save(S entity) {
		String raw = serialize(entity);
		String entityKey = redisKeyFromId(keyForEntity(entity));
		trackMembership(entityKey);
		redisOperations.boundHashOps(entityKey).put(redisHashKeyFromEntity(entity), raw);
		return entity;
	}

	@Override
	public void delete(RuntimeModuleInfoEntity entity) {
		String entityKey = redisKeyFromId(keyForEntity(entity));
		redisOperations.boundHashOps(entityKey).delete(redisHashKeyFromEntity(entity));
		if (redisOperations.boundHashOps(entityKey).entries().isEmpty()) {
			zSetOperations.remove(entityKey);
		}
	}

	@Override
	public Iterable<RuntimeModuleInfoEntity> findAll() {
		// This set is sorted
		Set<String> keys = zSetOperations.range(0, -1);

		List<RuntimeModuleInfoEntity> result = new ArrayList<RuntimeModuleInfoEntity>(keys.size());
		for (String entityKey : keys) {
			Map<String, String> entityValues = hashOperations.entries(entityKey);
			for (Map.Entry<String, String> entry : entityValues.entrySet()) {
				result.add(deserialize(entry.getKey(), entry.getValue()));
			}
		}
		return result;
	}

	@Override
	public Iterable<RuntimeModuleInfoEntity> findAll(Iterable<String> ids) {
		List<String> redisKeys = new ArrayList<String>();
		for (String id : ids) {
			redisKeys.add(redisKeyFromId(id));
		}
		List<RuntimeModuleInfoEntity> result = new ArrayList<RuntimeModuleInfoEntity>(redisKeys.size());
		for (String entityKey : redisKeys) {
			Map<String, String> entityValues = hashOperations.entries(entityKey);
			for (Map.Entry<String, String> entry : entityValues.entrySet()) {
				result.add(deserialize(entry.getKey(), entry.getValue()));
			}
		}
		return result;
	}

	@Override
	public Page<RuntimeModuleInfoEntity> findAll(Pageable pageable) {
		Assert.isNull(pageable.getSort(), "Arbitrary sorting is not implemented");
		long count = zSetOperations.size();
		// redis in inclusive on right side, hence -1
		long to = Math.min(count, pageable.getOffset() + pageable.getPageSize()) - 1;

		// But -1 means start from end, so cater for that
		Set<String> redisKeys = (to == -1) ? Collections.<String> emptySet() : zSetOperations.range(
				pageable.getOffset(), to);

		List<RuntimeModuleInfoEntity> result = new ArrayList<RuntimeModuleInfoEntity>(redisKeys.size());
		for (String entityKey : redisKeys) {
			Map<String, String> entityValues = hashOperations.entries(entityKey);
			for (Map.Entry<String, String> entry : entityValues.entrySet()) {
				result.add(deserialize(entry.getKey(), entry.getValue()));
			}
		}
		return new PageImpl<RuntimeModuleInfoEntity>(result, pageable, count);
	}

	@Override
	public RuntimeModuleInfoEntity findOne(String id) {
		throw new UnsupportedOperationException("Can't find a module entity by id");
	}

	@Override
	public Iterable<RuntimeModuleInfoEntity> findAllInRange(String from, boolean fromInclusive, String to, boolean toInclusive) {
		Set<String> keys = zSetOperations.range(0, -1);
		String fromRedis = redisKeyFromId(from);
		String toRedis = redisKeyFromId(to);
		Set<String> subSet = new TreeSet<String>(keys).subSet(fromRedis, fromInclusive, toRedis, toInclusive);

		List<RuntimeModuleInfoEntity> result = new ArrayList<RuntimeModuleInfoEntity>(subSet.size());
		for (String entityKey : subSet) {
			Map<String, String> entityValues = hashOperations.entries(entityKey);
			for (Map.Entry<String, String> entry : entityValues.entrySet()) {
				result.add(deserialize(entry.getKey(), entry.getValue()));
			}
		}
		return result;

	}
}
