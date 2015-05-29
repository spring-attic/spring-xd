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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.hamcrest.Description;
import org.hamcrest.DiagnosingMatcher;
import org.hamcrest.Matcher;

import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.xd.test.fixtures.util.FixtureUtils;

/**
 * Support class to capture output of a sink in a File.
 *
 * @author Eric Bottard
 * @author David Turanski
 */
public class FileSink extends DisposableFileSupport<FileSink> {


	/**
	 *  A matcher on the trimmed String contents of the sink, that delegates to another (String) matcher.
	 *   
	 *  @author David Turanski
	 */
	public static final class FileSinkTrimmedContentsMatcher extends FileSinkContentsMatcher {

		public FileSinkTrimmedContentsMatcher(Matcher<String> matcher) {
			super(matcher);
		}

		@Override
		protected boolean matches(Object item, Description mismatchDescription) {
			FileSink fs = (FileSink) item;
			try {
				String contents = fs.getContents().trim();
				mismatchDescription.appendValue(contents);
				return matcher.matches(contents);
			}
			catch (IOException e) {
				mismatchDescription.appendText("failed with an IOException: " + e.getMessage());
				return false;
			}
		}

	}

	/**
	 * A matcher on the String contents of the sink, that delegates to another (String) matcher.
	 *
	 * <p>
	 * Instances are to be constructed using
	 * {@code org.springframework.xd.shell.command.fixtures.XDMatchers#hasContentsThat}.
	 *
	 * @author Eric Bottard
	 */
	public static class FileSinkContentsMatcher extends DiagnosingMatcher<FileSink> {

		protected final Matcher<String> matcher;

		public FileSinkContentsMatcher(Matcher<String> matcher) {
			this.matcher = matcher;
		}

		@Override
		public void describeTo(Description description) {
			description.appendDescriptionOf(matcher);
		}

		@Override
		protected boolean matches(Object item, Description mismatchDescription) {
			FileSink fs = (FileSink) item;
			try {
				String contents = fs.getContents();
				mismatchDescription.appendValue(contents);
				return matcher.matches(contents);
			}
			catch (IOException e) {
				mismatchDescription.appendText("failed with an IOException: " + e.getMessage());
				return false;
			}
		}
	}

	private String charset = "UTF-8";

	private boolean binary = false;

	/**
	 * Create a file sink, but make sure that the future file is not present yet, so it can be waited upon with
	 * {@link #getContents()}.
	 */
	public FileSink() {
		Assert.state(file.delete());
	}

	/**
	 * Wait for the file to appear (default timeout) and return its contents.
	 */
	// This MUST remain private. Use XDMatchers.hasContentsThat() to assert
	private String getContents() throws IOException {
		return getContents(DEFAULT_FILE_TIMEOUT);
	}

	public FileSink binary(boolean binary) {
		this.binary = binary;
		return this;
	}

	/**
	 * Wait at most {@code timeout} ms for the file to appear and return its contents.
	 *
	 */
	// This MUST remain private. Use XDMatchers.hasContentsThat() to assert
	private String getContents(int timeout) throws IOException {
		if (waitFor(file, timeout)) {
			return FileCopyUtils.copyToString(new InputStreamReader(new FileInputStream(file), charset));
		}
		else {
			throw new IOException(String.format("Timeout after %dms waiting for file %s", timeout, file));
		}
	}

	@Override
	protected String toDSL() {
		String fileName = file.getName();
		return String.format("file --dir=%s --name=%s --suffix=%s --charset=%s --binary=%b --mode=%s",
				FixtureUtils.handleShellEscapeProcessing(file.getParent()),
				fileName.substring(0, fileName.lastIndexOf(".txt")), "txt", charset, binary, "APPEND");
	}

}
