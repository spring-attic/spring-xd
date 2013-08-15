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

package org.springframework.xd.store;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.repository.CrudRepository;

/**
 * Helper class for repositories, implementing some behavior in terms of unit methods. Implementations that can perform
 * operations in bulk are encouraged to override methods implemented here.
 * 
 * @author Eric Bottard
 */
public abstract class AbstractRepository<T, ID extends Serializable> implements CrudRepository<T, ID> {

	@Override
	public <S extends T> Iterable<S> save(Iterable<S> entities) {
		List<S> result = new ArrayList<S>();
		for (S entity : entities) {
			result.add(save(entity));
		}
		return result;
	}

	@Override
	public boolean exists(ID id) {
		return findOne(id) != null;
	}

	@Override
	public Iterable<T> findAll(Iterable<ID> ids) {
		List<T> result = new ArrayList<T>();
		for (ID id : ids) {
			T found = findOne(id);
			if (found != null) {
				result.add(found);
			}
		}
		return result;
	}

	@Override
	public void delete(Iterable<? extends T> entities) {
		for (T entity : entities) {
			delete(entity);
		}
	}

}
