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
public class TriggerDefinitionTests {

	@Test
	public void testToString() {
		final TriggerDefinition triggerDefinition = getTriggerDefinition();
		assertEquals("TriggerDefinition [name=myname, definition=mydef]",
				triggerDefinition.toString());
	}

	@Test
	public void testEquals() {
		final TriggerDefinition triggerDefinition1 = getTriggerDefinition();
		final TriggerDefinition triggerDefinition2 = getTriggerDefinition();

		assertFalse(triggerDefinition1 == triggerDefinition2);
		assertEquals(triggerDefinition1, triggerDefinition2);
	}

	@Test
	public void testNotEquals() {
		final TriggerDefinition triggerDefinition1 = new TriggerDefinition("myname", "mydef");
		final TriggerDefinition triggerDefinition2 = new TriggerDefinition("myname2", "mydef");

		assertFalse(triggerDefinition1 == triggerDefinition2);
		assertNotEquals(triggerDefinition1, triggerDefinition2);
	}

	@Test
	public void testComparable() {
		final TriggerDefinition triggerDefinition1 = new TriggerDefinition("aaa", "mydef");
		final TriggerDefinition triggerDefinition2 = new TriggerDefinition("bbb", "mydef");
		final TriggerDefinition triggerDefinition3 = new TriggerDefinition("ccc", "mydef");

		final SortedSet<TriggerDefinition> triggers = new TreeSet<TriggerDefinition>();

		triggers.add(triggerDefinition2);
		triggers.add(triggerDefinition1);
		triggers.add(triggerDefinition3);

		final Iterator<TriggerDefinition> iterator = triggers.iterator();

		assertEquals("aaa", iterator.next().getName());
		assertEquals("bbb", iterator.next().getName());
		assertEquals("ccc", iterator.next().getName());

	}

	private TriggerDefinition getTriggerDefinition() {
		return new TriggerDefinition("myname", "mydef");
	}

}
