/*
 * Copyright 2002-2013 the original author or authors.
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

import org.springframework.shell.core.CommandResult;
import org.springframework.shell.core.JLineShellComponent;

/**
 * Helper methods for tap commands to execute in the shell.
 * 
 * @author Mark Pollack
 */
public class TapCommandTemplate extends AbstractCommandTemplate {

	private List<String> taps = new ArrayList<String>();

	/**
	 * Construct a new TapCommandTemplate, given a spring shell.
	 * 
	 * @param shell the spring shell to execute commands against
	 */
	/* default */TapCommandTemplate(JLineShellComponent shell) {
		super(shell);
	}

	/**
	 * Destroy all taps that were created using the 'create' method. Commonly called in a @After annotated method.
	 */
	public void destroyCreatedTaps() {
		for (String tapname : taps) {
			destroy(tapname);
		}
	}

	public void destroy(String tapname) {
		CommandResult cr = executeCommand("tap destroy --name " + tapname);
		assertTrue("Failure to destory tap " + tapname + ".  CommandResult = " + cr.toString(), cr.isSuccess());
		assertEquals("Destroyed tap " + "'" + tapname + "'", cr.getResult());
	}

	/**
	 * Create and deploy a tap.
	 * 
	 * Note the name of the tap will be stored so that when the method destroyCreatedTaps is called, the stream will be
	 * destroyed.
	 * 
	 * @param tapname the name of the tap
	 * @param tapdefinition the tap definition DSL
	 * @param values will be injected in tapdefinition, using {@link String#format(String, Object...)} syntax
	 */
	public void create(String tapname, String tapdefinition, Object... values) {
		doCreate(tapname, tapdefinition, true, values);
	}

	/**
	 * Execute tap create (but don't deploy) for the supplied tap name/definition, and verify the command result.
	 * 
	 * Note the name of the stream will be stored so that when the method destroyCreateStreams is called, the stream
	 * will be destroyed.
	 * 
	 * @param tapname the name of the tap
	 * @param tapdefinition the tap definition DSL
	 * @param values will be injected in tapdefinition, using {@link String#format(String, Object...)} syntax
	 */
	public void createDontDeploy(String tapname, String tapdefinition, Object... values) {
		doCreate(tapname, tapdefinition, false, values);
	}

	private void doCreate(String tapname, String tapdefinition, boolean deploy, Object... values) {
		String actualDefinition = String.format(tapdefinition, values);
		String wholeCommand = String.format("tap create %s --definition \"%s\" --deploy %s", tapname, actualDefinition,
				deploy);
		// Save name before executing if something goes wrong
		taps.add(tapname);
		CommandResult cr = executeCommand(wholeCommand);
		String expectedResult = String.format("Created %snew tap '%s'", deploy ? "and deployed " : "", tapname);
		assertEquals(expectedResult, cr.getResult());
	}
}
