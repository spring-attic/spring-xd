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
 * A FileSource for integraiton tests. Note it does not need to implement {@link DisposableFileSupport}
 *
 * @author Glenn Renfro
 * @author Mark Pollack
 */
public class SimpleFileSource extends AbstractModuleFixture {

	private final String dir;

	private final String fileName;

	private boolean reference;


	/**
	 * Construct a new SimpleFileSource using the provided directory and file names.
	 *
	 * @param dir directory name
	 * @param fileName file name
	 * @param reference false if file content should be sent to output channel, true if file object should be sent.
	 */
	public SimpleFileSource(String dir, String fileName, boolean reference) {
		Assert.hasText(dir, "dir must not be null or empty");
		Assert.hasText(fileName, "fileName must not be null or empty");
		this.dir = dir;
		this.fileName = fileName;
		this.reference = reference;
	}

	/**
	 * Construct a new SimpleFileSource using the provided directory and file names.
	 *
	 * @param dir directory name
	 * @param fileName file name
	 */
	public SimpleFileSource(String dir, String fileName) {
		this(dir, fileName, false);
	}

	/**
	 * Renders the default DSL for this fixture.
	 */
	@Override
	protected String toDSL() {
		String result = "";
		if (label != null) {
			result = " " + label + ": ";
		}
		result += String.format("file --dir=%s --pattern='%s' --ref=%s", dir, fileName, reference);
		return result;
	}

	/**
	 * Determines if the file object or the content of the file should be sent to the output channel.
	 * @param reference Set to true to output the File object itself. False if content should be sent.
	 * @return the current instance of the SimpleFileSource fixture.
	 */
	public SimpleFileSource reference(boolean reference) {
		this.reference = reference;
		return this;
	}


}
