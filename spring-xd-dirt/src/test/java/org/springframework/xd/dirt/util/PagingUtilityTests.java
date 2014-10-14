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

package org.springframework.xd.dirt.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.util.Assert;


/**
 * Tests for {@link PagingUtility}.
 *
 * @author Ilayaperumal Gopinathan
 * @author Gunnar Hillert
 */
public class PagingUtilityTests {

	private static final PagingUtility<String> pagingUtility = new PagingUtility<String>();

	private static final List<String> data = Arrays.asList("a", "b", "c");

	/**
	 * Verify if the empty page is returned.
	 */
	@Test
	public void testEmptyPage() {
		PageRequest pageable = new PageRequest(0, 5);
		Page<String> page = pagingUtility.getPagedData(pageable, Collections.<String> emptyList());
		Assert.isTrue(page.getSize() == 0, "Page size for the empty list should be zero.");
	}

	/**
	 * Verify the first of N pages.
	 */
	@Test
	public void testFirstOfNPages() {
		int sliceSize = 2;
		PageRequest pageable = new PageRequest(0, sliceSize);
		Page<String> page = pagingUtility.getPagedData(pageable, data);
		verifyNthPage(page, Arrays.asList("a", "b"), sliceSize, 2);
		Assert.isTrue(page.hasNext());
		Assert.isTrue(page.isFirst());
	}

	/**
	 * Verify the second of N pages.
	 */
	@Test
	public void testSecondOfNPages() {
		int sliceSize = 1;
		PageRequest pageable = new PageRequest(1, sliceSize);
		Page<String> page = pagingUtility.getPagedData(pageable, data);
		verifyNthPage(page, Arrays.asList("b"), sliceSize, 3);
		Assert.isTrue(page.hasNext());
		Assert.isTrue(!page.isFirst() && !page.isLast());
	}

	/**
	 * Verify the last of N pages.
	 */
	@Test
	public void testLastOfNPages() {
		int sliceSize = 2;
		PageRequest pageable = new PageRequest(1, sliceSize);
		Page<String> page = pagingUtility.getPagedData(pageable, data);
		verifyNthPage(page, Arrays.asList("c"), sliceSize, 2);
		Assert.isTrue(!page.hasNext());
		Assert.isTrue(page.isLast());
	}

	/**
	 * Verify that a {@link PageNotFoundException} is raised in case a page is requested
	 * that does not exist.
	 */
	@Test(expected = PageNotFoundException.class)
	public void testInvalidPage() {
		int sliceSize = 2;
		PageRequest pageable = new PageRequest(5, sliceSize);
		pagingUtility.getPagedData(pageable, data);
	}

	/**
	 * Verify that a {@link PageNotFoundException} is raised in case a page is requested
	 * that does not exist and an empty collection is being passed-in.
	 */
	@Test(expected = PageNotFoundException.class)
	public void testInvalidPageWithEmptyCollection() {
		int sliceSize = 2;
		PageRequest pageable = new PageRequest(5, sliceSize);
		pagingUtility.getPagedData(pageable, Collections.<String> emptyList());
	}

	/**
	 * Verify that an {@link IllegalArgumentException} is raised in case a {@code null}
	 * collection is passed in.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testPassingInNullCollection() {
		int sliceSize = 2;
		PageRequest pageable = new PageRequest(5, sliceSize);
		pagingUtility.getPagedData(pageable, null);
	}

	/**
	 * Verify single page with the entire list.
	 */
	@Test
	public void testPageFullSize() {
		int sliceSize = 3;
		PageRequest pageable = new PageRequest(0, sliceSize);
		Page<String> page = pagingUtility.getPagedData(pageable, data);
		verifyNthPage(page, Arrays.asList("a", "b", "c"), sliceSize, 1);
		Assert.isTrue(!page.hasNext());
		Assert.isTrue(page.isFirst() && page.isLast());
	}

	/**
	 * Verify a specific page based on the given content and parameters.
	 *
	 * @param page the page to verify
	 * @param content the content of the page to check
	 * @param sliceSize the size of the slice in the page
	 * @param totalPages the total number of pages
	 */
	private void verifyNthPage(Page<String> page, List<String> content, int sliceSize, int totalPages) {
		Assert.isTrue(page.getContent().equals(content),
				"Page content should match the sliced list. Actual: " + page.getContent());
		Assert.isTrue(page.getSize() == sliceSize);
		Assert.isTrue(page.getTotalPages() == totalPages);
		Assert.isTrue(page.hasContent());
	}
}
