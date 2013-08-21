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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.data.repository.PagingAndSortingRepository;


/**
 * Base class for tests that apply to various repository implemenations.
 * 
 * @author Eric Bottard
 */
public abstract class BaseRepositoryTests<R extends PagingAndSortingRepository<String, Integer> & RangeCapableRepository<String, Integer>> {

	protected static final List<String> NUMBERS = Arrays.asList("zero", "one", "two", "three", "four", "five", "six",
			"seven", "eight", "nine");

	protected R repo;

	@Before
	public void setup() {
		repo = createRepository();
		populateRepository();
	}

	/**
	 * Subclasses must implement to init the repository.
	 */
	protected abstract R createRepository();

	@After
	public void tearDown() {
		repo.deleteAll();
	}

	@Test
	public void testRangeWithBoundariesPresent() {
		Iterable<String> result = repo.findAllInRange(3, true, 6, true);
		assertEquals(Arrays.asList("three", "four", "five", "six"), asList(result));

		result = repo.findAllInRange(3, false, 6, true);
		assertEquals(Arrays.asList("four", "five", "six"), asList(result));
	}

	@Test
	public void testRangeBoundariesNotPresent() {
		Iterable<String> result = repo.findAllInRange(8, true, 16, true);
		assertEquals(Arrays.asList("eight", "nine"), asList(result));

	}

	@Test
	public void testOutside() {
		Iterable<String> result = repo.findAllInRange(99, true, 101, true);
		assertFalse(result.iterator().hasNext());
	}

	@Test
	public void testRangeSingleSelection() {
		Iterable<String> result = repo.findAllInRange(8, true, 8, true);
		assertEquals(Arrays.asList("eight"), asList(result));
		result = repo.findAllInRange(8, true, 8, false);
		assertEquals(Arrays.asList(), asList(result));
		result = repo.findAllInRange(8, false, 8, true);
		assertEquals(Arrays.asList(), asList(result));
		result = repo.findAllInRange(8, false, 8, false);
		assertEquals(Arrays.asList(), asList(result));
	}

	protected <T> List<T> asList(Iterable<T> iterable) {
		List<T> result = new ArrayList<T>();
		for (T e : iterable) {
			result.add(e);
		}
		return result;
	}


	protected void populateRepository() {
		List<String> shuffled = new ArrayList<String>(NUMBERS);
		Collections.shuffle(shuffled);
		for (String n : shuffled) {
			repo.save(n);
		}

	}

}
