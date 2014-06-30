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
import org.springframework.util.StringUtils;
import org.springframework.xd.test.fixtures.util.FixtureUtils;


/**
 * Used to generate the file sink portion of a stream. Used by Acceptance testing because it does not extend disposable.
 *
 * @author Glenn Renfro
 */
public class SimpleFileSink extends AbstractModuleFixture<SimpleFileSink> {

	public final static String DEFAULT_MODE = "REPLACE";

	private String dir;

	private String fileName;

	private boolean binary = true;

	private String mode;

	/**
	 * Use XD Defaults for the file name and directory to where the sink will write its results.
	 */
	public SimpleFileSink() {
		this(null, null, true, DEFAULT_MODE);
	}

	/**
	 * Construct a new SimpleFileSink given the directory and file to write to as well as the if the file is binary
	 * and the mode that determines what to do if the file already exists. 
	 *
	 * @param dir The name of the directory where the output file will be placed.
	 * @param fileName the name of the file.
	 * @param binary if true the not add a linefeed to the end of the line.  if false a linefeed will be added to the end of the line.
	 * @param mode determines if the file should be REPLACE, APPEND, etc 
	 */
	public SimpleFileSink(String dir, String fileName, boolean binary, String mode) {
		Assert.hasText(mode, "mode must not be null nor empty");
		this.dir = dir;
		this.fileName = fileName;
		this.binary = binary;
		this.mode = mode;
	}

	/**
	 * Return an instance of a SimpleFileSink given the directory and file to write to.  The file mode is binary and 
	 * will be replaced if it already exists.
	 * @param dir the directory where files should be written
	 * @param fileName the name of the file to be written.
	 * @return an instance of SimpleFileSink
	 */
	public static SimpleFileSink withDefaults(String dir, String fileName) {
		return new SimpleFileSink(dir, fileName, true, DEFAULT_MODE);
	}

	/**
	 * Renders the default DSL for this fixture.
	 */
	@Override
	public String toDSL() {
		String dsl = FixtureUtils.labelOrEmpty(label);
		dsl += String.format("file --binary=%s --mode=%s ", binary, mode);
		if (StringUtils.hasText(fileName)) {
			dsl += "--name='" + fileName + "' ";
		}
		if (StringUtils.hasText(dir)) {
			dsl += "--dir='" + dir + "' ";
		}
		return dsl;
	}

	/**
	 * Establishes the directory the SimpleFileSink will write the file.
	 * @param dir the name of the directory
	 * @return the current instance of the SimpleFileSink
	 */
	public SimpleFileSink dir(String dir) {
		this.dir = dir;
		return this;
	}

	/**
	 * Establishes the filename the SimpleFileSink will associate with the file.
	 * @param fileName the name for the file
	 * @return the current instance of the SimpleFileSink
	 */
	public SimpleFileSink fileName(String fileName) {
		this.fileName = fileName;
		return this;
	}

	/**
	 * Set the state of the binary flag for the SimpleFileSink instance.
	 * @param binary if true the not add a linefeed to the end of the line.  if false a linefeed will be added to the end of the line.
	 * @return the current instance of the SimpleFileSink
	 */
	public SimpleFileSink binary(boolean binary) {
		this.binary = binary;
		return this;
	}

	/**
	 * Establishes the mode in which the SimpleFileSink.  
	 * @param mode determines if the file existing file should be REPLACE, APPEND, etc 
	 * @return the current instance of the simple file sink.
	 */
	public SimpleFileSink mode(String mode) {
		Assert.hasText(mode, "monde can not be null nor empty");
		this.mode = mode;
		return this;
	}


}
