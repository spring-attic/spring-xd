/*
 * Copyright 2013-2015 the original author or authors.
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
import org.springframework.xd.test.fixtures.util.FixtureUtils;


/**
 * A FileSource for integration tests. Note it does not need to implement {@link DisposableFileSupport}
 *
 * @author Glenn Renfro
 * @author Mark Pollack
 * @author Gunnar Hillert
 */
public class SimpleFileSource extends AbstractModuleFixture<SimpleFileSource> {

	private final String dir;

	private final String fileName;

	private Mode mode;


	/**
	 * Construct a new SimpleFileSource using the provided directory and file names.
	 *
	 * @param dir directory name
	 * @param fileName file name
	 * @param mode 'FILE_AS_BYTES' if file content should be sent to output channel, 'REF' if file object should be sent, 'TEXT_LINE' for splitting the file into Strings (1 per line).
	 */
	public SimpleFileSource(String dir, String fileName, Mode mode) {
		Assert.hasText(dir, "dir must not be null or empty");
		Assert.hasText(fileName, "fileName must not be null or empty");
		Assert.notNull(mode, "mode must not be null");
		this.dir = dir;
		this.fileName = fileName;
		this.mode = mode;
	}

	/**
	 * Construct a new SimpleFileSource using the provided directory and file names.
	 *
	 * @param dir directory name
	 * @param fileName file name
	 */
	public SimpleFileSource(String dir, String fileName) {
		this(dir, fileName, Mode.CONTENTS);
	}

	/**
	 * Renders the default DSL for this fixture.
	 */
	@Override
	protected String toDSL() {
		String dsl = FixtureUtils.labelOrEmpty(label);
		dsl += String.format("file --dir=%s --pattern='%s' --mode=%s", dir, fileName, mode);
		return dsl;
	}

	/**
	 * Determines if the file object or the content of the file should be sent to the output channel.
	 * @param mode 'FILE_AS_BYTES' if file content should be sent to output channel, 'REF' if file object should be sent, 'TEXT_LINE' for splitting the file into Strings (1 per line).
	 * @return the current instance of the SimpleFileSource fixture.
	 */
	public SimpleFileSource mode(Mode mode) {
		this.mode = mode;
		return this;
	}

	public enum Mode {

		REF("ref"),
		LINES("lines"),
		CONTENTS("contents");

		private String value;

		/**
		 * Constructor.
		 */
		Mode(final String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}

	}
}
