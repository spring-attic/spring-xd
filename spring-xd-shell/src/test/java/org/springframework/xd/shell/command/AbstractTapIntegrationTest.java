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

/**
 * Provides an @After JUnit lifecycle method that will destroy the taps that
 * were created by calling executeTapCreate
 * 
 * @author Ilayaperumal Gopinathan
 * 
 */
public abstract class AbstractTapIntegrationTest extends
		AbstractStreamIntegrationTest {

	private List<String> taps = new ArrayList<String>();

	@After
	public void after() {
		executeTapDestroy(taps.toArray(new String[taps.size()]));
		super.after();
	}

	/**
	 * Execute 'tap destroy' for the supplied tap names
	 */
	private void executeTapDestroy(String... tapnames) {
		for (String tapname : tapnames) {
			CommandResult cr = executeCommand("tap destroy --name " + tapname);
			assertTrue("Failure to destory tap " + tapname
					+ ".  CommandResult = " + cr.toString(), cr.isSuccess());
		}
	}

	protected void executeTapCreate(String tapname, String tapdefinition) {
		executeTapCreate(tapname, tapdefinition, true);
	}

	/**
	 * Execute tap create for the supplied tap name/definition, and verify the
	 * command result.
	 */
	protected void executeTapCreate(String tapname, String tapdefinition,
			boolean deploy) {
		CommandResult cr = executeCommand("tap create --definition \""
				+ tapdefinition + "\" --name " + tapname
				+ (deploy ? "" : " --deploy false"));
		assertEquals((deploy ? "Created and deployed new tap "
				: "Created new tap ") + "'"+tapname+"'", cr.getResult());
		taps.add(tapname);
	}

}
