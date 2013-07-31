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
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.springframework.shell.core.CommandResult;
import org.springframework.xd.shell.AbstractShellIntegrationTest;

/**
 * Provides an @After JUnit lifecycle method that will destroy the streams that were 
 * created by calling executeStreamCreate
 * 
 * @author Andy Clement
 * @author Mark Pollack
 *
 */
public abstract class AbstractStreamIntegrationTest extends AbstractShellIntegrationTest {

	
	private List<String> streams = new ArrayList<String>();
	
	@After
	public void after() {
		executeStreamDestroy(streams.toArray(new String[streams.size()]));
	}

	/**
	 * Execute 'stream destroy' for the supplied stream names
	 */
	private void executeStreamDestroy(String... streamnames) {
		for (String streamname: streamnames) {
			CommandResult cr = executeCommand("stream destroy --name "+streamname);
			assertTrue("Failure to destory stream " + streamname + ".  CommandResult = " + cr.toString(), cr.isSuccess());
		}
	}

	protected void executeStreamCreate(String streamname, String streamdefinition) {
		executeStreamCreate(streamname, streamdefinition, true);
	}

	/**
	 * Execute stream create for the supplied stream name/definition, and verify
	 * the command result.
	 */
	protected void executeStreamCreate(String streamname, String streamdefinition, boolean deploy) {
		CommandResult cr = executeCommand("stream create --definition \""+
			streamdefinition+"\" --name "+streamname+
				(deploy?"":" --deploy false"));
		assertEquals("Created new stream '"+streamname+"'",cr.getResult());
		streams.add(streamname);
	}

}
