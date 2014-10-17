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

package org.springframework.xd.analytics.metrics.core;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;


/**
 * Unit tests for the {@link MetricUtils} class.
 *
 * @author Eric Bottard
 */
public class MetricUtilsTests {

	private List<long[]> data = new ArrayList<long[]>();

	@Before
	public void setup() {
		data.add(new long[] {1, 2, 3, 4, 5});
		data.add(new long[] {6, 7, 8, 9, 10, 11, 12, 13});
		data.add(new long[] {14, 15, 16});

	}

	@Test(expected = IndexOutOfBoundsException.class)
	public void testNotEnoughData() {
		List<long[]> data = new ArrayList<long[]>();
		data.add(new long[] {1, 2, 3, 4, 5});

		MetricUtils.concatArrays(data, 3, 3);
	}

	@Test
	public void testSpanChunks() {

		long[] result = MetricUtils.concatArrays(data, 3, 6);
		assertArrayEquals(new long[] {4, 5, 6, 7, 8, 9}, result);
	}

}
