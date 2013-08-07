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

import java.util.ArrayList;
import java.util.List;

import org.springframework.shell.core.CommandResult;
import org.springframework.shell.core.JLineShellComponent;

import static org.junit.Assert.*;

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
	public TapCommandTemplate(JLineShellComponent shell) {
		super(shell);
	}

	/**
	 * Destroy all taps that were created using the 'create' method. Commonly called in a @After
	 * annotated method.
	 */
	public void destroyCreatedTaps() {
		for (String tapname : taps) {
			CommandResult cr = executeCommand("tap destroy --name " + tapname);
			assertTrue("Failure to destory tap " + tapname + ".  CommandResult = " + cr.toString(), cr.isSuccess());
		}
	}

	/**
	 * Create and deploy a tap
	 * 
	 * Note the name of the tap will be stored so that when the method destroyCreatedTaps
	 * is called, the stream will be destroyed.
	 * 
	 * @param tapname the name of the tap
	 * @param tapdefinition the tap definition DSL
	 */
	public void create(String tapname, String tapdefinition) {
		create(tapname, tapdefinition, true);
	}

	/**
	 * Execute tap create for the supplied tap name/definition, and verify the command
	 * result.
	 * 
	 * Note the name of the stream will be stored so that when the method
	 * destroyCreateStreams is called, the stream will be destroyed.
	 * 
	 * @param tapname the name of the tap
	 * @param tapdefinition the tap definition DSL
	 * @param deploy deploy the stream if true, otherwise just create the definition
	 */
	public void create(String tapname, String tapdefinition, boolean deploy) {
		CommandResult cr = executeCommand("tap create --definition \"" + tapdefinition + "\" --name " + tapname
				+ (deploy ? "" : " --deploy false"));
		taps.add(tapname);
		String expectedResult = String.format("Created %snew tap '%s'", deploy ? "and deployed " : "", tapname);
		assertEquals(expectedResult, cr.getResult());
	}
}
