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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;

/**
 * Support class to capture output of a sink in a File.
 *
 * @author Eric Bottard
 */
class FileSink extends DisposableFileSupport {

	private String charset = "UTF-8";

	/**
	 * Create a file sink, but make sure that the future file is not present yet, so it can be waited upon with {@link #getContents()}.
	 */
	public FileSink() {
		Assert.state(file.delete());
	}

	/**
	 * Wait for the file to appear (default timeout) and return its contents.
	 */
	public String getContents() throws IOException {
		return getContents(DEFAULT_FILE_TIMEOUT);
	}

	/**
	 * Wait at most {@code timeout} ms for the file to appear and return its contents.
	 */
	public String getContents(int timeout) throws IOException {
		waitFor(file, timeout);
		Reader fileReader = new InputStreamReader(new FileInputStream( file), charset);
		return FileCopyUtils.copyToString(fileReader);
	}

	@Override
	protected String toDSL() {
		String fileName = file.getName();
		return String.format("file --dir=%s --name=%s --suffix=%s --charset=%s", file.getParent(),
				fileName.substring(0, fileName.lastIndexOf(".txt")), "txt", charset);
	}

}