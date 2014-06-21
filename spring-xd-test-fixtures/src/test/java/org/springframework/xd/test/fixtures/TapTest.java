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

package org.springframework.xd.test.fixtures;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;


/**
 * Unit tests for the Tap Fixture
 * 
 * @author Glenn Renfro
 */
public class TapTest {

	private static final String STREAM_NAME = "stream-name";

	private Tap tap;

	@Before
	public void initialize() {
		tap = new Tap(STREAM_NAME);
	}

	/**
	 * Test method for {@link org.springframework.xd.test.fixtures.Tap#toDSL()}.
	 */
	@Test
	public void testToDSL() {
		String dsl = tap.toDSL();
		assertEquals("tap:stream:" + STREAM_NAME, dsl);
		dsl = tap.label("FOO").toDSL();
		assertEquals("tap:stream:" + STREAM_NAME + ".FOO", dsl);
		dsl = tap.label("BAR").toDSL();
		assertEquals("tap:stream:" + STREAM_NAME + ".BAR", dsl);
	}

	/**
	 * Test method for null Stream parameter into constructor {@link org.springframework.xd.test.fixtures.Tap#Tap(java.lang.String)}.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testNullStream() {
		new Tap(null);
	}

	/**
	 * Test method for empty label parameter into constructor {@link org.springframework.xd.test.fixtures.Tap#Tap(java.lang.String)}.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testEmptyStream() {
		new Tap("");
	}


	/**
	 * Test method for null label parameter into label method {@link org.springframework.xd.test.fixtures.Tap#label(java.lang.String)}.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testNullLabel() {
		tap.label(null);
	}

	/**
	 * Test method for empty label parameter into label method {@link org.springframework.xd.test.fixtures.Tap#label(java.lang.String)}.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testEmptyLabel() {
		tap.label("");
	}


}
