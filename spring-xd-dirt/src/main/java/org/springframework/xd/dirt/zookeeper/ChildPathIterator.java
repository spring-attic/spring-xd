/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.xd.dirt.zookeeper;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;

import org.springframework.core.convert.converter.Converter;
import org.springframework.util.Assert;

/**
 * Iterator over {@link ChildData} instances that are managed by {@link PathChildrenCache}. A {@link Converter} is used
 * to convert the node data into a domain object.
 * 
 * @param <T> the domain class type returned by this iterator
 * 
 * @author Patrick Peralta
 */
public class ChildPathIterator<T> implements Iterator<T> {

	/**
	 * Converter from {@link ChildData} to domain class type {@link T}.
	 */
	private final Converter<ChildData, T> converter;

	/**
	 * The actual iterator over the {@link ChildData}.
	 */
	private final Iterator<ChildData> iterator;

	/**
	 * Construct a ChildPathIterator.
	 * 
	 * @param converter converter from node data to domain object
	 * @param cache source for children nodes
	 */
	public ChildPathIterator(Converter<ChildData, T> converter, PathChildrenCache cache) {
		Assert.notNull(converter);
		Assert.notNull(cache);

		this.converter = converter;
		List<ChildData> list = cache.getCurrentData();
		if (list == null) {
			list = Collections.<ChildData> emptyList();
		}
		this.iterator = list.iterator();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean hasNext() {
		return iterator.hasNext();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public T next() {
		return converter.convert(iterator.next());
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * Implementation throws {@link UnsupportedOperationException} because removals are not supported.
	 */
	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

}
