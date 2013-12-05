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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import org.springframework.shell.core.CommandResult;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.shell.util.Table;
import org.springframework.xd.shell.util.TableRow;
import org.springframework.xd.shell.util.UiUtils;

/**
 * Test module commands.
 * 
 * @author Glenn Renfro
 * @author Gunnar Hillert
 * @author Mark Fisher
 */
public class ModuleCommandTests extends AbstractStreamIntegrationTest {

	@Test
	public void testListAll() throws InterruptedException {
		Table t = listAll();
		assertTrue("trigger source is not present in module list command",
				t.getRows().contains(new TableRow().addValue(1, "trigger").addValue(2, "source")));
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
		assertTrue(t.getRows().contains(new TableRow().addValue(1, "trigger").addValue(2, "source")));
		assertFalse(t.getRows().contains(new TableRow().addValue(1, "splunk").addValue(2, "sink")));
	}

	@Test
	public void testListForSink() throws InterruptedException {
		Table t = listByType("sink");
		assertTrue("splunk module is missing from sink list",
				t.getRows().contains(new TableRow().addValue(1, "splunk").addValue(2, "sink")));
		assertFalse("time module is should not be sink list",
				t.getRows().contains(new TableRow().addValue(1, "time").addValue(2, "source")));

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
		assertNull("Invalid Type will return a null set", t);
	}

	@Test
	public void testModuleCompose() {
		compose().newModule("compositesource", "time | splitter");


		Table t = listByType("source");
		assertTrue("compositesource is not present in list",
				t.getRows().contains(new TableRow().addValue(1, "compositesource").addValue(2, "source")));

	}

	@Test
	public void testCollidingModuleComposeWithOtherComposite() {
		compose().newModule("compositesource", "time | splitter");

		CommandResult result = getShell().executeCommand(
				"module compose compositesource --definition \"time | transform\"");
		assertEquals("There is already a module named 'compositesource' with type 'source'\n",
				result.getException().getMessage());

	}

	@Test
	public void testCollidingModuleComposeWithRegularModule() {
		CommandResult result = getShell().executeCommand(
				"module compose tcp --definition \"time | transform\"");
		assertEquals("There is already a module named 'tcp' with type 'source'\n", result.getException().getMessage());

	}

	@Test
	public void testAttemptToDeleteNonComposedModule() {
		assertFalse(compose().delete("tcp", ModuleType.source));
	}

	@Test
	public void testDeleteUnusedComposedModule() {
		compose().newModule("myhttp", "http | filter");
		assertTrue(compose().delete("myhttp", ModuleType.source));
	}

	@Test
	public void testDeleteComposedModuleUsedByOtherModule() {
		compose().newModule("myhttp", "http | filter");
		compose().newModule("evenbetterhttp", "myhttp | transform");
		assertFalse(compose().delete("myhttp", ModuleType.source));

		// Now delete blocking module
		assertTrue(compose().delete("evenbetterhttp", ModuleType.source));
		assertTrue(compose().delete("myhttp", ModuleType.source));
	}

	@Test
	public void testDeleteComposedModuleUsedByStream() {
		compose().newModule("myhttp", "http | filter");
		executeCommand("stream create foo --definition \"myhttp | log\" --deploy false");
		assertFalse(compose().delete("myhttp", ModuleType.source));
		// Now deleting blocking stream
		executeCommand("stream destroy foo");
		assertTrue(compose().delete("myhttp", ModuleType.source));
	}

	private Table listAll() {
		return (Table) getShell().executeCommand("module list").getResult();
	}

	private Table listByType(String type) {
		return (Table) getShell().executeCommand("module list --type " + type).getResult();
	}

	@Test
	public void testDisplayConfigurationFile() throws InterruptedException {

		final CommandResult commandResult = getShell().executeCommand(
				String.format("module display source:file"));

		assertTrue("The status of the command result should be successfuly", commandResult.isSuccess());
		assertNotNull("The configurationFile should not be null.", commandResult.getResult());
		assertNull("We should not get an exception returned.", commandResult.getException());

		final String result = (String) commandResult.getResult();
		assertTrue("The configuration file should start with the XML header.",
				result.startsWith(
						"Configuration file contents for module definiton 'file' (source):\n\n"
								+ UiUtils.HORIZONTAL_LINE
								+ "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));

	}

	@Test
	public void testDisplayNonExistingConfigurationFile() throws InterruptedException {
		final CommandResult commandResult = getShell().executeCommand(
				String.format("module display source:blubbadoesnotexist"));
		assertFalse("The status of the command result should be successful", commandResult.isSuccess());
		assertNotNull("We should get an exception returned.", commandResult.getException());
		assertEquals("Could not find module with name 'blubbadoesnotexist' and type 'source'\n",
				commandResult.getException().getMessage());

	}
}
