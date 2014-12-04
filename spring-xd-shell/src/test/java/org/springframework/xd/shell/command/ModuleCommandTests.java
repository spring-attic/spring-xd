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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.springframework.xd.module.ModuleType.processor;
import static org.springframework.xd.module.ModuleType.source;
import static org.springframework.xd.module.core.CompositeModule.OPTION_SEPARATOR;
import static org.springframework.xd.shell.command.fixtures.XDMatchers.eventually;
import static org.springframework.xd.shell.command.fixtures.XDMatchers.hasContentsThat;

import java.io.File;
import java.io.IOException;

import org.hamcrest.Description;
import org.hamcrest.DiagnosingMatcher;
import org.hamcrest.Matcher;
import org.junit.Test;

import org.springframework.shell.core.CommandResult;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.shell.command.fixtures.HttpSource;
import org.springframework.xd.shell.util.Table;
import org.springframework.xd.shell.util.TableRow;
import org.springframework.xd.test.fixtures.FileSink;

/**
 * Test module commands.
 *
 * @author Glenn Renfro
 * @author Gunnar Hillert
 * @author Mark Fisher
 * @author David Turanski
 * @author Eric Bottard
 */
public class ModuleCommandTests extends AbstractStreamIntegrationTest {

	@Test
	public void testModuleCompose() {
		module().compose("compositesource", "time | splitter");

		Table t = listAll();

		assertThat(t.getRows(), hasItem(rowWithValue(1, "(c) compositesource")));
	}

	/**
	 * This tests that options passed in the definition of a composed module are kept as first level defaults.
	 */
	@Test
	public void testComposedModulesValuesInDefinition() throws IOException {
		FileSink sink = newFileSink().binary(true);
		HttpSource httpSource = newHttpSource();
		module().compose("filterAndTransform",
				"filter --expression=true | transform --expression=payload.replace('abc','...')");
		stream().create(generateStreamName(), "%s | filterAndTransform | %s", httpSource, sink);
		httpSource.ensureReady().postData("abcdefghi!");
		assertThat(sink, eventually(hasContentsThat(equalTo("...defghi!"))));
	}

	/**
	 * This tests that options passed at usage time of a composed module are override definition values.
	 */
	@Test
	public void testComposedModulesValuesAtUsageTime() throws IOException {
		FileSink sink = newFileSink().binary(true);
		HttpSource httpSource = newHttpSource();
		module().compose("filterAndTransform",
				"filter --expression=false | transform --expression=payload.replace('abc','...')");
		String options = String.format(
				"--filter%sexpression=true --transform%sexpression=payload.replace('def','...')", OPTION_SEPARATOR,
				OPTION_SEPARATOR);
		stream().create(generateStreamName(), "%s | filterAndTransform %s | %s", httpSource, options, sink);
		httpSource.ensureReady().postData("abcdefghi!");
		assertThat(sink, eventually(hasContentsThat(equalTo("abc...ghi!"))));

	}

	@Test
	public void testCollidingModuleComposeWithOtherComposite() {
		module().compose("compositesource", "time | splitter");

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
		assertFalse(module().delete("tcp", ModuleType.source));
	}

	@Test
	public void testDeleteUnusedComposedModule() {
		module().compose("myhttp", "http | filter");
		assertTrue(module().delete("myhttp", ModuleType.source));
	}

	@Test
	public void testDeleteComposedModuleUsedByOtherModule() {
		module().compose("myhttp", "http | filter");
		module().compose("evenbetterhttp", "myhttp | transform");
		assertFalse(module().delete("myhttp", ModuleType.source));

		// Now delete blocking module
		assertTrue(module().delete("evenbetterhttp", ModuleType.source));
		assertTrue(module().delete("myhttp", ModuleType.source));
	}

	@Test
	public void testDeleteComposedModuleUsedByStream() {
		module().compose("myhttp", "http | filter");
		executeCommand("stream create foo --definition \"myhttp | log\" --deploy false");
		assertFalse(module().delete("myhttp", ModuleType.source));
		// Now deleting blocking stream
		executeCommand("stream destroy foo");
		assertTrue(module().delete("myhttp", ModuleType.source));
	}

	@Test
	public void testModuleUpload() {
		module().upload("siDslModule2", processor, new File("src/test/resources/spring-xd/xd/modules/processor/siDslModule.jar"));

		Table t = listAll();

		assertThat(t.getRows(), hasItem(rowWithValue(2, "    siDslModule2")));

	}

	@Test
	public void testModuleUploadClashing() {
		try {
			module().upload("http", source, new File("src/test/resources/spring-xd/xd/modules/processor/siDslModule.jar"));
			fail("Should have failed uploading module");
		}
		catch (AssertionError error) {
			assertThat(error.getMessage(), containsString("There is already a module named 'http' with type 'source'"));
		}
	}

	@Test
	public void testDeleteUploadedModuleUsedByStream() {
		module().upload("siDslModule2", processor, new File("src/test/resources/spring-xd/xd/modules/processor/siDslModule.jar"));
		executeCommand("stream create foo --definition \"http | siDslModule2 --prefix=foo | log\" --deploy false");
		assertFalse(module().delete("siDslModule2", processor));
		// Now deleting blocking stream
		executeCommand("stream destroy foo");
		assertTrue(module().delete("siDslModule2", processor));
	}

	private Table listAll() {
		return (Table) getShell().executeCommand("module list").getResult();
	}

	private Matcher<TableRow> rowWithValue(final int col, final String value) {
		return new DiagnosingMatcher<TableRow>() {

			@Override
			public void describeTo(Description description) {
				description.appendText("a row with ").appendValue(value);
			}

			@Override
			protected boolean matches(Object item, Description mismatchDescription) {
				String actualValue = ((TableRow) item).getValue(col);
				mismatchDescription.appendText("a row with ").appendValue(actualValue);
				return value.equals(actualValue);
			}
		};
	}


}
