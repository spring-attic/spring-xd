/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.xd.dirt.stream;

import static org.junit.Assert.*;

import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.Test;

/**
 *
 * @author Gunnar Hillert
 * @since 1.0
 *
 */
public class TapDefinitionTests {

	@Test
	public void testToString() {
		final TapDefinition tapDefinition = getTapDefinition();
		System.out.print(tapDefinition);
	}

	@Test
	public void testEquals() {
		final TapDefinition tapDefinition1 = getTapDefinition();
		final TapDefinition tapDefinition2 = getTapDefinition();

		assertFalse(tapDefinition1 == tapDefinition2);
		assertEquals(tapDefinition1, tapDefinition2);
	}

	@Test
	public void testNotEquals() {
		final TapDefinition tapDefinition1 = new TapDefinition("mytapname", "mystreamname1", "mydef");
		final TapDefinition tapDefinition2 = new TapDefinition("mytapname", "mystreamname2", "mydef");

		assertFalse(tapDefinition1 == tapDefinition2);
		assertNotEquals(tapDefinition1, tapDefinition2);
	}

	@Test
	public void testComparable() {
		final TapDefinition tapDefinition1 = new TapDefinition("aaa", "mystreamname1", "mydef");
		final TapDefinition tapDefinition2 = new TapDefinition("bbb", "mystreamname2", "mydef");
		final TapDefinition tapDefinition3 = new TapDefinition("ccc", "mystreamname2", "mydef");

		final SortedSet<TapDefinition> taps = new TreeSet<TapDefinition>();

		taps.add(tapDefinition2);
		taps.add(tapDefinition1);
		taps.add(tapDefinition3);

		final Iterator<TapDefinition> iterator = taps.iterator();

		assertEquals("aaa", iterator.next().getName());
		assertEquals("bbb", iterator.next().getName());
		assertEquals("ccc", iterator.next().getName());

	}

	private TapDefinition getTapDefinition() {
		return new TapDefinition("mytapname", "mystreamname", "mydef");
	}

}
