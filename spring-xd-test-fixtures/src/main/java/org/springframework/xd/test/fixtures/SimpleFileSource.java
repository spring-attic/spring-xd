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

	/**
	 * Construct a new SimpleFileSource in the current directory with the name "SimpleFileSource"
	 */
	public SimpleFileSource() {
		this("", SimpleFileSource.class.getName());
	}

	/**
	 * Construct a new SimpleFileSource using the provided directory and file names.
	 * 
	 * @param dir directory name
	 * @param fileName file name
	 */
	public SimpleFileSource(String dir, String fileName) {
		Assert.hasText(dir, "dir must not be null or empty");
		Assert.hasText(fileName, "fileName must not be null or empty");
		this.dir = dir;
		this.fileName = fileName;
	}

	@Override
	protected String toDSL() {
		return String.format("file --dir=%s --pattern='%s'", dir, fileName);
	}


}
