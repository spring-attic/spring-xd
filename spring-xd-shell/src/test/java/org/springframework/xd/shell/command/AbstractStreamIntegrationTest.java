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

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.springframework.shell.core.CommandResult;
import org.springframework.xd.shell.AbstractShellIntegrationTest;
import org.springframework.xd.shell.util.Table;
import org.springframework.xd.shell.util.TableRow;

import static org.junit.Assert.*;

/**
 * Provides an @After JUnit lifecycle method that will destroy the definitions that were created by calling
 * executeXXXCreate methods.
 *
 * @author Andy Clement
 * @author Mark Pollack
 *
 */
public abstract class AbstractStreamIntegrationTest extends AbstractShellIntegrationTest {

	private List<String> streams = new ArrayList<String>();

	private List<String> taps = new ArrayList<String>();

	@After
	public void after() {
		executeStreamDestroy(streams.toArray(new String[streams.size()]));
		executeTapDestroy(taps.toArray(new String[taps.size()]));
	}

	/**
	 * Execute 'tap destroy' for the supplied tap names.
	 */
	private void executeTapDestroy(String[] tapnames) {
		for (String tapname : tapnames) {
			CommandResult cr = executeCommand("tap destroy --name " + tapname);
			assertTrue("Failure to destory tap " + tapname + ".  CommandResult = " + cr.toString(), cr.isSuccess());
		}
	}

	/**
	 * Execute 'stream destroy' for the supplied stream names.
	 */
	private void executeStreamDestroy(String... streamnames) {
		for (String streamname : streamnames) {
			CommandResult cr = executeCommand("stream destroy --name " + streamname);
			assertTrue("Failure to destory stream " + streamname + ".  CommandResult = " + cr.toString(),
					cr.isSuccess());
		}
	}

	protected void executeStreamCreate(String streamname, String streamdefinition) {
		executeStreamCreate(streamname, streamdefinition, true);
	}

	protected void executeTapCreate(String tapname, String tapdefinition) {
		executeTapCreate(tapname, tapdefinition, true);
	}

	/**
	 * Execute stream create for the supplied stream name/definition, and verify the command result.
	 */
	protected void executeStreamCreate(String streamname, String streamdefinition, boolean deploy) {
		CommandResult cr = executeCommand("stream create --definition \""+
			streamdefinition+"\" --name "+streamname+
				(deploy?"":" --deploy false"));
		// add the stream name to the streams list before assertion
		streams.add(streamname);
		assertEquals("Created new stream '"+streamname+"'",cr.getResult());
		verifyStreamExists(streamname, streamdefinition);
	}

	protected void executeStreamDeploy(String streamname) {
		CommandResult cr = getShell().executeCommand("stream deploy --name "+streamname);
		assertTrue("Failure.  CommandResult = " + cr.toString(), cr.isSuccess());
		assertEquals("Deployed stream 'ticktock'", cr.getResult());
	}

	protected void executeStreamUndeploy(String streamname) {
		CommandResult cr = getShell().executeCommand("stream undeploy --name ticktock");
		assertTrue(cr.isSuccess());
		assertEquals("Un-deployed stream 'ticktock'", cr.getResult());
	}

	/**
	 * Execute tap create for the supplied tap name/definition, and verify the command result.
	 */
	protected void executeTapCreate(String tapname, String tapdefinition, boolean deploy) {
		CommandResult cr = executeCommand("tap create --definition \"" + tapdefinition + "\" --name " + tapname
				+ (deploy ? "" : " --deploy false"));
		taps.add(tapname);
		assertEquals("Created new tap '" + tapname + "'", cr.getResult());
	}

	/**
	 * Verify the stream is listed in stream list
	 * @param streamName the name of the stream
	 * @param definition definition of the stream
	 */
	protected void verifyStreamExists(String streamName, String definition){
		CommandResult cr = getShell().executeCommand("stream list");
		assertTrue("Failure.  CommandResult = " + cr.toString(), cr.isSuccess());
		Table t = (Table) cr.getResult();
		assertTrue(t.getRows().contains(new TableRow().addValue(1, streamName).addValue(2, definition)));
	}

}
