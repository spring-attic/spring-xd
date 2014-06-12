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

package org.springframework.xd.test.fixtures;

import org.springframework.shell.core.JLineShellComponent;


/**
 * Base class for Metrics related sinks, makes sure named metric is deleted at end of test.
 * 
 * @author Eric Bottard
 * @author Glenn Renfro
 */
public class AbstractMetricSink extends AbstractModuleFixture<AbstractMetricSink> implements
		Disposable {

	private final String name;

	private final String dslName;

	final public JLineShellComponent shell;


	public AbstractMetricSink(JLineShellComponent shell, String name, String dslName) {
		this.shell = shell;
		this.name = name;
		this.dslName = dslName;
	}

	@Override
	public void cleanup() {
		// Do not fail here if command fails, as this is typically called alongside
		// other cleanup code
		shell.executeCommand(String.format("%s delete --name %s", dslName, name));
	}

	@Override
	protected String toDSL() {
		return String.format("%s --name=%s", dslName, name);
	}


	public String getName() {
		return name;
	}


	public String getDslName() {
		return dslName;
	}


}
