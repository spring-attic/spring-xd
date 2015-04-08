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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.shell.core.CommandResult;
import org.springframework.shell.core.JLineShellComponent;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.test.fixtures.Disposable;


/**
 * Issues commands related to module composition and upload. Also remembers created modules, so that they can be cleaned up.
 * 
 * @author Eric Bottard
 */
public class ModuleTemplate implements Disposable {

	private static final Pattern COMPOSE_SUCCESS_PATTERN = Pattern.compile("Successfully created module '(.+)' with type (.+)");

	private static final Pattern UPLOAD_SUCCESS_PATTERN = Pattern.compile("Successfully uploaded module '(.+):(.+)'");

	private final JLineShellComponent shell;

	public ModuleTemplate(JLineShellComponent shell) {
		this.shell = shell;
	}

	/**
	 * Remembers created modules, in the form <type>:<name>.
	 */
	private List<String> modules = new ArrayList<>();

	public String upload(String name, ModuleType type, File file) {
		String escapedPath = file.getAbsolutePath().replace("\\", "\\\\");
		CommandResult result = shell.executeCommand(String.format("module upload --type %s --name %s --file '%s'", type, name, escapedPath));
		assertNotNull("Module upload apparently failed. Exception is: " + result.getException(),
				result.getResult());
		Matcher matcher = UPLOAD_SUCCESS_PATTERN.matcher((CharSequence) result.getResult());
		assertTrue("Module upload apparently failed: " + result.getResult(), matcher.matches());
		String key = matcher.group(1) + ":" + matcher.group(2);
		modules.add(key);
		return key;
	}

	public boolean delete(String name, ModuleType type) {
		CommandResult result = shell.executeCommand(String.format("module delete %s:%s", type, name));
		return result.isSuccess();
	}

	public String info(String name, ModuleType type) {
		CommandResult result = shell.executeCommand(String.format("module info %s:%s", type, name));
		return (String) result.getResult();
	}

	@Override
	public void cleanup() {
		Collections.reverse(modules);
		for (String m : modules) {
			String[] parts = m.split(":");
			ModuleType type = ModuleType.valueOf(parts[0]);
			String name = parts[1];
			delete(name, type);
		}
	}

	public String compose(String name, String definition) {
		return this.compose(name, definition, false);
	}

	public String compose(String name, String definition, boolean force) {
		CommandResult result = shell.executeCommand(String.format("module compose %s --definition \"%s\" --force %b", name,
				definition, force));
		assertNotNull("Module composition apparently failed. Exception is: " + result.getException(),
				result.getResult());
		Matcher matcher = COMPOSE_SUCCESS_PATTERN.matcher((CharSequence) result.getResult());
		assertTrue("Module composition apparently failed: " + result.getResult(), matcher.matches());
		String key = matcher.group(2) + ":" + matcher.group(1);
		modules.add(key);
		return key;
	}
}
