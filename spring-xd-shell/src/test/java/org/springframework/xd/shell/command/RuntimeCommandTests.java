/*
 * Copyright 2013 the original author or authors.
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
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import org.springframework.shell.core.CommandResult;
import org.springframework.util.StringUtils;
import org.springframework.xd.shell.util.Table;
import org.springframework.xd.shell.util.TableRow;


/**
 * Runtime commands tests
 * 
 * @author Ilayaperumal Gopinathan
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
			assertTrue(!StringUtils.isEmpty(row.getValue(2)) && !StringUtils.isEmpty(row.getValue(3)));
		}
		// Verify there should be at least one container in the list.
		assertTrue(table.getRows().size() > 0);
	}

	@Test
	public void testListRuntimeModules() {
		logger.info("List runtime modules");
		String streamName = generateStreamName();
		stream().create(streamName, "time | log");
		CommandResult cmdResult = executeCommand("runtime modules");
		Table table = (Table) cmdResult.getResult();
		List<TableRow> fooStreamModules = new ArrayList<TableRow>();
		for (TableRow row : table.getRows()) {
			// match by group name
			if (row.getValue(2).equals(streamName)) {
				fooStreamModules.add(row);
			}
		}
		assertEquals(2, fooStreamModules.size());
	}

	@Test
	public void testListRuntimeModulesAfterUndeploy() {
		logger.info("List runtime modules after undeploy");
		String streamName = generateStreamName();
		stream().create(streamName, "time | log");
		stream().undeploy(streamName);
		CommandResult cmdResult = executeCommand("runtime modules");
		Table table = (Table) cmdResult.getResult();
		List<TableRow> fooStreamModules = new ArrayList<TableRow>();
		for (TableRow row : table.getRows()) {
			// match by group name
			if (row.getValue(2).equals(streamName)) {
				fooStreamModules.add(row);
			}
		}
		assertEquals(0, fooStreamModules.size());
	}

	@Test
	public void testListRuntimeModulesByContainerId() {
		logger.info("Test listing of runtime modules by containerId");
		String streamName = generateStreamName();
		stream().create(streamName, "time | log");

		Table table = (Table) executeCommand("runtime modules").getResult();
		List<TableRow> runtimeModules = new ArrayList<TableRow>();
		for (TableRow row : table.getRows()) {
			// match by group name
			if (row.getValue(2).equals(streamName)) {
				runtimeModules.add(row);
			}
		}
		// Get containerId for a runtime module
		String containerId = runtimeModules.get(0).getValue(1);
		CommandResult cmdResult = executeCommand("runtime modules --containerId " + containerId);
		Table table1 = (Table) cmdResult.getResult();
		List<TableRow> fooStreamModules = new ArrayList<TableRow>();
		for (TableRow row : table1.getRows()) {
			// Verify all the rows have the same containerId
			assertTrue(row.getValue(1).equals(containerId));
			// match by group name
			if (row.getValue(2).equals(runtimeModules.get(0).getValue(2))) {
				fooStreamModules.add(row);
			}
		}
		// Verify the module is listed for the containerId
		assertTrue(!fooStreamModules.isEmpty());
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
}
