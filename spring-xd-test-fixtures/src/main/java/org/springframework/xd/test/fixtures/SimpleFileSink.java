/*
 * Copyright 2013-2014 the original author or authors.
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
 * Used to generate the file sink portion of a stream. Used by Acceptance testing because it does not extend disposable.
 *
 * @author Glenn Renfro
 */
public class SimpleFileSink extends AbstractModuleFixture {

	private String dir;

	private String fileName;

	/**
	 * Use XD Defaults for the file name and directory to where the sink will write its results.
	 */
	public SimpleFileSink() {
		this(null, null);
	}

	/**
	 * Allows a user to setup a simple file sink so that the results will be written to the directory/file specified.
	 *
	 * @param dir The name of the directory where the output file will be placed.
	 * @param fileName the name of the file.
	 */
	public SimpleFileSink(String dir, String fileName) {
		if (dir != null) {
			Assert.hasText(dir);
		}
		if (fileName != null) {
			Assert.hasText(fileName);
		}
		this.dir = dir;
		this.fileName = fileName;
	}

	/**
	 * Renders the default DSL for this fixture.
	 */
	@Override
	protected String toDSL() {
		return String.format("file --binary=true --mode=%s", "REPLACE");
	}

	/**
	 * Renders the DSL for this fixture using the file write mode and binary true or false.
	 *
	 * @param mode What action should the sink take if a file exists already. (REPLACE, APPEND, FAIL, IGNORE)
	 * @param binary false if you want append a newline to each line. True if no newline is to be added to each new
	 *        line.
	 * @return sink portion of a stream definition.
	 */
	public String toDSL(String mode, String binary) {
		Assert.hasText(mode, "mode must not be empty nor null");
		Assert.hasText(binary, "binary must not be empty nor null");

		String result = String.format("file  --mode=%s --binary=%s ", mode, binary);
		if (fileName != null) {
			result += "--name='" + fileName + "' ";
		}
		if (dir != null) {
			result += "--dir='" + dir + "'";
		}
		return result;
	}

}
