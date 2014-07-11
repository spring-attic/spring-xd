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

package org.springframework.xd.analytics.ml;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.xd.tuple.Tuple;
import org.springframework.xd.tuple.TupleBuilder;

/**
 * @author Thomas Darimont
 */
public class MappedAnalyticTest {

	Analytic<Tuple, Tuple> analytic;

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Before
	public void setup() {
		analytic = new DummyMappedAnalytic(new DummyInputMapper(), new DummyOutputMapper());
	}

	/**
	 * @see XD-1418
	 */
	@Test
	public void testShouldNotAllowNullInputMapper() {

		expectedException.expect(IllegalArgumentException.class);
		expectedException.expectMessage("inputMapper");

		new DummyMappedAnalytic(null, new DummyOutputMapper());
	}

	/**
	 * @see XD-1418
	 */
	@Test
	public void testShouldNotAllowNullOutputMapper() {

		expectedException.expect(IllegalArgumentException.class);
		expectedException.expectMessage("outputMapper");

		new DummyMappedAnalytic(new DummyInputMapper(), null);
	}

	/**
	 * @see XD-1418
	 */
	@Test
	public void testEvaluateDummyMappedAnalytic() {

		Tuple input = TupleBuilder.tuple().of("k1", "v1", "k2", "v2");

		Tuple output = analytic.evaluate(input);

		assertNotSame(input, output);
		assertThat(output.getString("k1"), is("V1"));
		assertThat(output.getString("k2"), is("V2"));
	}
}
