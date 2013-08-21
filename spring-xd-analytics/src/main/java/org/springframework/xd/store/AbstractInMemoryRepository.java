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

package org.springframework.xd.store;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.util.Assert;

/**
 * Base implementation for an in-memory store, using a {@link Map} internally.
 * 
 * Default behaviour is to retain sort order on the keys. Hence, this is by default the only sort supported when
 * querying with a {@link Pageable}.
 * 
 * @param <T> the type of things to store
 * @param <ID> a "primary key" to the things
 * @author Eric Bottard
 */
public abstract class AbstractInMemoryRepository<T, ID extends Serializable & Comparable<ID>> extends
		AbstractRepository<T, ID>
		implements PagingAndSortingRepository<T, ID>, RangeCapableRepository<T, ID> {

	private final NavigableMap<ID, T> map;

	protected AbstractInMemoryRepository() {
		map = buildMap();
	}

	protected NavigableMap<ID, T> buildMap() {
		return new ConcurrentSkipListMap<ID, T>();
	}

	@Override
	public <S extends T> S save(S entity) {
		Assert.notNull(entity);
		map.put(safeKeyFor(entity), entity);
		return entity;
	}

	private final ID safeKeyFor(T entity) {
		ID k = keyFor(entity);
		Assert.notNull(k);
		return k;
	}

	protected abstract ID keyFor(T entity);

	@Override
	public T findOne(ID id) {
		Assert.notNull(id);
		return map.get(id);
	}

	@Override
	public Iterable<T> findAll() {
		return new ArrayList<T>(map.values());
	}

	@Override
	public long count() {
		return map.size();
	}

	@Override
	public void delete(ID id) {
		Assert.notNull(id);
		map.remove(id);
	}

	@Override
	public void delete(T entity) {
		Assert.notNull(entity);
		map.remove(safeKeyFor(entity));
	}

	@Override
	public void deleteAll() {
		map.clear();
	}

	@Override
	public Page<T> findAll(Pageable pageable) {
		Assert.isNull(pageable.getSort(), "Arbitrary sorting is not implemented");
		return slice((List<T>) findAll(), pageable);
	}

	/**
	 * Post-process the list to only return elements matching the page request.
	 */
	protected Page<T> slice(List<T> list, Pageable pageable) {
		int to = Math.min(list.size(), pageable.getOffset() + pageable.getPageSize());
		List<T> data = list.subList(pageable.getOffset(), to);
		return new PageImpl<T>(data, pageable, list.size());
	}

	@Override
	public Iterable<T> findAll(Sort sort) {
		throw new UnsupportedOperationException("Arbitrary sorting is not implemented");
	}

	@Override
	public Iterable<T> findAllInRange(ID from, boolean fromInclusive, ID to, boolean toInclusive) {
		return map.subMap(from, fromInclusive, to, toInclusive).values();
	}

}
