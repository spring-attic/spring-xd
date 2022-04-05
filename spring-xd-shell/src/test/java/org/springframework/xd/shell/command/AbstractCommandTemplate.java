/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.shell.command;

import org.springframework.shell.core.CommandResult;
import org.springframework.shell.core.JLineShellComponent;
import org.springframework.util.Assert;
import org.springframework.xd.dirt.test.ResourceStateVerifier;

/**
 * Common operations to execute in the shell.
 *
 * @author Mark Pollack
 * @author David TUranski
 */
public abstract class AbstractCommandTemplate {

	protected static final String DEFAULT_METRIC_NAME = "bar";

	private JLineShellComponent shell;

	protected final ResourceStateVerifier stateVerifier;

	public AbstractCommandTemplate(JLineShellComponent shell, ResourceStateVerifier stateVerifier) {
		this.shell = shell;
		this.stateVerifier = stateVerifier;
	}

	/**
	 * Execute a command and verify the command result.
	 */
	protected CommandResult executeCommand(String command) {
		CommandResult cr = getShell().executeCommand(command);
		if (cr.getException() != null) {
			cr.getException().printStackTrace();
		}
		Assert.isTrue(cr.isSuccess(), "Failure.  CommandResult = " + cr.toString());
		return cr;
	}

	protected JLineShellComponent getShell() {
		return shell;
	}

}
