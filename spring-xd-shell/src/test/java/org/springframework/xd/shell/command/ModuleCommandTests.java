/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.shell.command;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import org.springframework.xd.shell.AbstractShellIntegrationTest;
import org.springframework.xd.shell.util.Table;
import org.springframework.xd.shell.util.TableRow;

/**
 * Test stream commands
 * 
 * @author Glenn Renfro
 * @author Gunnar Hillert
 */
public class ModuleCommandTests extends AbstractShellIntegrationTest {

	private static final Log logger = LogFactory.getLog(ModuleCommandTests.class);

	@Test
	public void testListAll() throws InterruptedException {
		Table t = listAll();
		assertTrue("cron-trigger source is not present in module list command",
				t.getRows().contains(new TableRow().addValue(1, "cron-trigger").addValue(2, "source")));
		assertTrue("file source is not present in module list command",
				t.getRows().contains(new TableRow().addValue(1, "file").addValue(2, "source")));
		assertTrue("splitter processor is not present in module list command",
				t.getRows().contains(new TableRow().addValue(1, "splitter").addValue(2, "processor")));
		assertTrue("splunk sink is not present in module list command",
				t.getRows().contains(new TableRow().addValue(1, "splunk").addValue(2, "sink")));
	}

	@Test
	public void testListForSource() throws InterruptedException {
		Table t = listByType("source");
		assertTrue(t.getRows().contains(new TableRow().addValue(1, "cron-trigger").addValue(2, "source")));
		assertFalse(t.getRows().contains(new TableRow().addValue(1, "splunk").addValue(2, "sink")));
	}

	@Test
	public void testListForSink() throws InterruptedException {
		Table t = listByType("sink");
		assertTrue("splunk module is missing from sink list",
				t.getRows().contains(new TableRow().addValue(1, "splunk").addValue(2, "sink")));
		assertFalse("file module should not be present int sink list",
				t.getRows().contains(new TableRow().addValue(1, "file").addValue(2, "source")));
	}

	@Test
	public void testListForProcessor() throws InterruptedException {
		Table t = listByType("processor");
		assertTrue("Splitter Processor is not present in list",
				t.getRows().contains(new TableRow().addValue(1, "splitter").addValue(2, "processor")));
		assertFalse("Processor list should not contain a source module",
				t.getRows().contains(new TableRow().addValue(1, "file").addValue(2, "source")));
	}

	@Test
	public void testListForInvalidType() throws InterruptedException {
		Table t = listByType("foo");
		assertTrue("Invalid Type should return no values", t.getRows().size() == 0);
	}

	private Table listAll() {
		return (Table) getShell().executeCommand("module list").getResult();
	}

	private Table listByType(String type) {
		return (Table) getShell().executeCommand("module list --type " + type).getResult();
	}
}
