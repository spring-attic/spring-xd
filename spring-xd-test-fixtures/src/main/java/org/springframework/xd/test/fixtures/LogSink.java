/*
 * Copyright 2014 the original author or authors.
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

import org.springframework.util.Assert;


/**
 * A test fixture that represents the log sink
 *
 * @author Glenn Renfro
 */
public class LogSink extends AbstractModuleFixture<LogSink> {

	String moduleName;

	/**
	 * Establishes the module name for the log.
	 *
	 * @param moduleName the name you want to assign to the log module.
	 */
	public LogSink(String moduleName) {
		Assert.hasText(moduleName, "moduleName must not be empty nor null");

		this.moduleName = moduleName;
	}

	/**
	 * Renders the DSL for this fixture.
	 */
	@Override
	protected String toDSL() {
		return String.format("log --name=%s", moduleName);
	}

}
