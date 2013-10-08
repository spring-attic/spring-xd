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
import static org.junit.Assert.assertNotNull;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import org.springframework.shell.core.CommandResult;
import org.springframework.xd.shell.AbstractShellIntegrationTest;
import org.springframework.xd.shell.util.Table;
import org.springframework.xd.shell.util.TableRow;


/**
 * Container commands tests
 * 
 * @author Ilayaperumal Gopinathan
 */
public class ContainerCommandTests extends AbstractShellIntegrationTest {

	private static final Log logger = LogFactory.getLog(ContainerCommandTests.class);

	@Test
	public void testListContainers() {
		logger.info("List the runtime containers");
		CommandResult cmdResult = executeCommand("container list");
		Table table = (Table) cmdResult.getResult();
		assertEquals(1, table.getRows().size());
		TableRow row = table.getRows().get(0);
		assertNotNull(row);
		// Check container Id
		assertEquals("0", row.getValue(1));
		// Verify hostname is not null
		assertNotNull(row.getValue(2));
		// Verify IP address is not null
		assertNotNull(row.getValue(3));
	}

}
