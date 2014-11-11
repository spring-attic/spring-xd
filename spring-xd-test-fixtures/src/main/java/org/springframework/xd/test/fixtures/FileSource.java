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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.springframework.xd.test.fixtures.util.FixtureUtils;

/**
 * Support class that represents the file source module.
 *
 * @author Eric Bottard
 * @author David Turanski
 */
public class FileSource extends DisposableFileSupport<FileSource> {

	public FileSource() {
		super(makeDir());
	}

	/**
	 * First make a temporary directory where our file will live.
	 */
	private static File makeDir() {
		try {
			File dir = File.createTempFile("FileSource", "");
			dir.delete();
			dir.mkdirs();
			return dir;
		}
		catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	protected String toDSL() {
		return String.format("file --outputType=text/plain --dir=%s",
				FixtureUtils.handleShellEscapeProcessing(file.getParent()));
	}

	public void appendToFile(String contents) throws IOException {
		FileWriter fileWritter = new FileWriter(file, true);
		BufferedWriter bufferWritter = new BufferedWriter(fileWritter);
		bufferWritter.write(contents);
		bufferWritter.close();
	}

	@Override
	public void cleanup() {
		// first delete file inside dir
		super.cleanup();
		// then dir itself
		file.getParentFile().delete();
	}

}
