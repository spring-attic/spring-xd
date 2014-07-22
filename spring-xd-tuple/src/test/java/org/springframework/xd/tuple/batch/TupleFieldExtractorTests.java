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

package org.springframework.xd.tuple.batch;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.xd.tuple.Tuple;
import org.springframework.xd.tuple.TupleBuilder;

public class TupleFieldExtractorTests {

	private TupleFieldExtractor extractor;

	@Rule
	public ExpectedException expected = ExpectedException.none();

	@Before
	public void setUp() {
		extractor = new TupleFieldExtractor();
	}

	@Test
	@SuppressWarnings("serial")
	public void testExtractWithFields() throws Exception {
		extractor.setNames(new ArrayList<String>() {

			{
				add("first");
				add("last");
				add("born");
			}
		});

		String first = "Alan";
		String last = "Turing";
		int born = 1912;

		Tuple item = TupleBuilder.tuple()
				.put("first", first)
				.put("last", last)
				.put("born", born)
				.build();

		Object[] values = extractor.extract(item);

		assertEquals(3, values.length);
		assertEquals(first, values[0]);
		assertEquals(last, values[1]);
		assertEquals(born, values[2]);
	}

	@Test
	public void testExtractWithoutFields() throws Exception {

		String first = "Alan";
		String last = "Turing";
		int born = 1912;

		Tuple item = TupleBuilder.tuple()
				.put("first", first)
				.put("last", last)
				.put("born", born)
				.build();

		Object[] values = extractor.extract(item);

		assertEquals(3, values.length);
		assertEquals(first, values[0]);
		assertEquals(last, values[1]);
		assertEquals(born, values[2]);
	}


	@Test
	@SuppressWarnings("serial")
	public void testExtract_invalidProperty() throws Exception {
		expected.expect(IllegalArgumentException.class);
		expected.expectMessage("Field name [birthday] does not exist");

		extractor.setNames(new ArrayList<String>() {

			{
				add("first");
				add("last");
				add("birthday");
			}
		});

		String first = "Alan";
		String last = "Turing";
		int born = 1912;

		Tuple item = TupleBuilder.tuple()
				.put("first", first)
				.put("last", last)
				.put("born", born)
				.build();

		extractor.extract(item);
	}
}
