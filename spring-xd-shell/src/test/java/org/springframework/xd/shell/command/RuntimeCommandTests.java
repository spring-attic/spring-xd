/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.xd.shell.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.springframework.xd.shell.command.fixtures.XDMatchers.eventually;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hamcrest.Description;
import org.hamcrest.DiagnosingMatcher;
import org.junit.Test;

import org.springframework.shell.core.CommandResult;
import org.springframework.util.StringUtils;
import org.springframework.xd.shell.util.Table;
import org.springframework.xd.shell.util.TableRow;


/**
 * Runtime commands tests.
 *
 * @author Ilayaperumal Gopinathan
 * @author Patrick Peralta
 */
public class RuntimeCommandTests extends AbstractStreamIntegrationTest {

	private static final Log logger = LogFactory.getLog(RuntimeCommandTests.class);

	@Test
	public void testListContainers() {
		logger.info("List runtime containers");
		CommandResult cmdResult = executeCommand("runtime containers");
		Table table = (Table) cmdResult.getResult();
		for (TableRow row : table.getRows()) {
			// Verify host name & ip address are not empty
			assertTrue(StringUtils.hasText(row.getValue(2)) && StringUtils.hasText(row.getValue(3)));
		}
		// Verify there should be at least one container in the list.
		assertTrue(table.getRows().size() > 0);
	}

	@Test
	public void testListRuntimeModules() {
		logger.info("List runtime modules");
		String streamName = generateStreamName();
		stream().create(streamName, "time | log");

		assertThat(2, eventually(moduleCountForStream(streamName)));
	}

	@Test
	public void testListRuntimeModulesByModuleId() {
		logger.info("List runtime modules");
		String streamName = generateStreamName();
		stream().create(streamName, "time | log");
		CommandResult cmdResult = executeCommand("runtime modules --moduleId " + streamName + ".sink.log.1");
		Table table = (Table) cmdResult.getResult();
		assertEquals(1, table.getRows().size());
	}

	@Test
	public void testListRuntimeModulesAfterUndeploy() {
		logger.info("List runtime modules after undeploy");
		String streamName = generateStreamName();
		stream().create(streamName, "time | log");
		stream().undeploy(streamName);

		assertThat(0, eventually(moduleCountForStream(streamName)));
	}

	@Test
	public void testListRuntimeModulesByContainerId() {
		logger.info("Test listing of runtime modules by containerId");
		String streamName = generateStreamName();
		stream().create(streamName, "time | log");

		assertThat(2, eventually(moduleCountForStream(streamName)));

		List<TableRow> runtimeModules = getRuntimeModulesForStream(streamName);

		// Get containerId for a runtime module
		String containerId = runtimeModules.get(0).getValue(2);
		CommandResult cmdResult = executeCommand("runtime modules --containerId " + containerId);
		Table table = (Table) cmdResult.getResult();
		List<TableRow> fooStreamModules = new ArrayList<TableRow>();
		for (TableRow row : table.getRows()) {
			// Verify all the rows have the same containerId
			assertTrue(row.getValue(2).equals(containerId));
			// match by group name
			if (row.getValue(2).equals(runtimeModules.get(0).getValue(2))) {
				fooStreamModules.add(row);
			}
		}
		// Verify the module is listed for the containerId
		assertFalse(fooStreamModules.isEmpty());
	}

	@Test
	public void testListRuntimeModulesByInvalidContainerId() {
		logger.info("Test listing of runtime modules by invalid containerId");
		String streamName = generateStreamName();
		stream().create(streamName, "time | log");
		CommandResult cmdResult = executeCommand("runtime modules --containerId 10000");
		Table table = (Table) cmdResult.getResult();
		assertEquals(0, table.getRows().size());
	}

	/**
	 * Return a list of runtime modules for the stream name.
	 *
	 * @param streamName name of stream for which to obtain runtime modules
	 * @return list of table rows containing runtime module information
	 *         for the requested stream
	 */
	private List<TableRow> getRuntimeModulesForStream(String streamName) {
		CommandResult cmdResult = executeCommand("runtime modules");
		Table table = (Table) cmdResult.getResult();
		List<TableRow> modules = new ArrayList<TableRow>();
		for (TableRow row : table.getRows()) {
			if (row.getValue(1).contains(streamName)) {
				modules.add(row);
			}
		}
		return modules;
	}

	/**
	 * Return a matcher that determines if the number of deployed modules
	 * for a stream matches the expected value.
	 *
	 * @param streamName name of stream for which to obtain number of deployed modules
	 * @return matcher that determines if the number of modules matches
	 */
	private ModuleCountMatcher moduleCountForStream(String streamName) {
		return new ModuleCountMatcher(streamName);
	}


	/**
	 * Matcher that determines if the number of deployed modules for
	 * a stream matches the expected value.
	 */
	class ModuleCountMatcher extends DiagnosingMatcher<Integer> {

		/**
		 * Name of stream for which to obtain number of deployed modules.
		 */
		private final String streamName;

		/**
		 * Construct a {@code ModuleCountMatcher}.
		 * @param streamName name of stream for which to obtain
		 *                   number of deployed modules.
		 */
		ModuleCountMatcher(String streamName) {
			this.streamName = streamName;
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * Return true if the integer (passed in as {@code item})
		 * matches the number of deployed modules.
		 * </p>
		 */
		@Override
		protected boolean matches(Object item, Description description) {
			int size = (Integer) item;
			List<TableRow> list = getRuntimeModulesForStream(streamName);
			if (list.size() == size) {
				return true;
			}
			else {
				description.appendText("Expected ")
						.appendValue(size)
						.appendText(" number of modules for stream ")
						.appendText(streamName)
						.appendText(" in list ")
						.appendValue(list)
						.appendText("; instead there are ")
						.appendValue(list.size())
						.appendText(System.lineSeparator());
				return false;
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void describeTo(Description description) {
			description.appendText("number of modules for stream ")
					.appendText(streamName);
		}
	}

}
