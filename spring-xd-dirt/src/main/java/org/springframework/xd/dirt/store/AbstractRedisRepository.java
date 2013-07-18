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

package org.springframework.xd.dirt.store;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.BoundZSetOperations;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.util.Assert;

/**
 * Base implementation for a store, using Redis behind the scenes. This implementation requires a {@code repoPrefix},
 * that is used in two ways:
 * <ul>
 * <li>a sorted set is stored under that exact key that tracks the entity ids this repository is responsible for,
 * <li>
 * <li>each entity is stored serialized under key {@code repoPrefix<id>}</li>
 * </ul>
 * 
 * @param <T> the type of things to store
 * @param <ID> a "primary key" to the things
 * @author Eric Bottard
 */
public abstract class AbstractRedisRepository<T, ID extends Serializable> implements PagingAndSortingRepository<T, ID> {

	private String repoPrefix;

	private BoundZSetOperations<String, String> zSetOperations;

	private RedisOperations<String, String> redisOperations;

	public AbstractRedisRepository(String repoPrefix, RedisOperations<String, String> redisOperations) {
		Assert.hasText(repoPrefix, "repoPrefix must not be empty or null");
		Assert.notNull(redisOperations, "redisOperations must not be null");
		this.redisOperations = redisOperations;
		setPrefix(repoPrefix);
	}

	@Override
	public long count() {
		return zSetOperations.size();
	}

	@Override
	public void delete(ID id) {
		if (zSetOperations.remove(redisKeyFromId(id))) {
			redisOperations.delete(redisKeyFromId(id));
		}
	}

	@Override
	public void delete(Iterable<? extends T> entities) {
		for (T entity : entities) {
			delete(keyFor(entity));
		}
	}

	@Override
	public void delete(T entity) {
		delete(keyFor(entity));
	}

	@Override
	public void deleteAll() {
		Set<String> keys = zSetOperations.range(0, -1);
		zSetOperations.removeRange(0, -1);
		redisOperations.delete(keys);
	}

	@Override
	public boolean exists(ID id) {
		return redisOperations.hasKey(redisKeyFromId(id));
	}

	@Override
	public Iterable<T> findAll() {
		Set<String> keys = zSetOperations.range(0, -1);
		List<T> result = new ArrayList<T>(keys.size());
		List<String> values = redisOperations.opsForValue().multiGet(keys);
		for (String v : values) {
			result.add(deserialize(v));
		}
		return result;
	}

	@Override
	public Iterable<T> findAll(Iterable<ID> ids) {
		List<String> redisKeys = new ArrayList<String>();
		for (ID id : ids) {
			redisKeys.add(redisKeyFromId(id));
		}
		List<T> result = new ArrayList<T>(redisKeys.size());
		List<String> values = redisOperations.opsForValue().multiGet(redisKeys);
		for (String v : values) {
			result.add(deserialize(v));
		}
		return result;
	}

	@Override
	public Page<T> findAll(Pageable pageable) {
		Assert.isNull(pageable.getSort(), "Arbitrary sorting is not implemented");
		long count = zSetOperations.size();
		// redis in inclusive on right side, hence -1
		long to = Math.min(count, pageable.getOffset() + pageable.getPageSize()) - 1;

		// But -1 means start from end, so cater for that
		Set<String> redisKeys = (to == -1) ? Collections.<String> emptySet() : zSetOperations.range(
				pageable.getOffset(), to);

		List<T> result = new ArrayList<T>(redisKeys.size());
		List<String> values = redisOperations.opsForValue().multiGet(redisKeys);
		for (String v : values) {
			result.add(deserialize(v));
		}
		return new PageImpl<T>(result, pageable, count);
	}

	@Override
	public Iterable<T> findAll(Sort sort) {
		throw new UnsupportedOperationException("Can't sort on arbitrary property");
	}

	@Override
	public T findOne(ID id) {
		String raw = redisOperations.opsForValue().get(redisKeyFromId(id));
		if (raw != null) {
			return deserialize(raw);
		}
		else {
			return null;
		}
	}

	@Override
	public <S extends T> S save(S entity) {
		String raw = serialize(entity);
		String redisKey = redisKeyFromId(keyFor(entity));
		zSetOperations.add(redisKey, 0.0D);
		redisOperations.opsForValue().set(redisKey, raw);
		return entity;
	}

	@Override
	public <S extends T> Iterable<S> save(Iterable<S> entities) {
		for (S entity : entities) {
			save(entity);
		}
		return entities;
	}

	/**
	 * Deserialize from the String representation to the domain object.
	 */
	protected abstract T deserialize(String v);

	/**
	 * Provide a String representation of the domain entity.
	 */
	protected abstract String serialize(T entity);

	/**
	 * Return the entity id for the given domain object.
	 */
	protected abstract ID keyFor(T entity);

	/**
	 * Return a String representation of the domain ID.
	 */
	protected abstract String serializeId(ID id);

	private String redisKeyFromId(ID id) {
		return repoPrefix + serializeId(id);
	}

	/**
	 * Change the prefix after creation. Mainly intented for testing.
	 */
	public void setPrefix(String string) {
		this.repoPrefix = string;
		this.zSetOperations = redisOperations.boundZSetOps(repoPrefix);
	}

}
