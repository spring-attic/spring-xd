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

package org.springframework.xd.shell.command.fixtures;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.hamcrest.Description;
import org.hamcrest.DiagnosingMatcher;
import org.hamcrest.Matcher;

import org.springframework.util.FileCopyUtils;
import org.springframework.xd.test.fixtures.AbstractMetricSink;
import org.springframework.xd.test.fixtures.EventuallyMatcher;
import org.springframework.xd.test.fixtures.FileSink;
import org.springframework.xd.test.fixtures.FileSink.FileSinkContentsMatcher;
import org.springframework.xd.test.fixtures.FileSink.FileSinkTrimmedContentsMatcher;
import org.springframework.xd.test.fixtures.HasDisplayValue;
import org.springframework.xd.test.fixtures.MetricHasSimpleValueMatcher;


/**
 * Utility class that provides static factory methods for {@link Matcher}s in this package.
 *
 * @author Eric Bottard
 * @author David Turanski
 */
public class XDMatchers {

	private XDMatchers() {

	}

	public static <T extends AbstractMetricSink> MetricExistsMatcher<T> exists() {
		return new MetricExistsMatcher<T>();
	}

	public static <U, T extends AbstractMetricSink & HasDisplayValue<U>> MetricHasSimpleValueMatcher<U, T> hasValue(
			U expectedValue) {
		return new MetricHasSimpleValueMatcher<U, T>(expectedValue);
	}

	public static <U> EventuallyMatcher<U> eventually(Matcher<U> matcher) {
		return new EventuallyMatcher<U>(matcher);
	}

	public static <U> EventuallyMatcher<U> eventually(int nbAttempts, int pause, Matcher<U> matcher) {
		return new EventuallyMatcher<U>(matcher, nbAttempts, pause);
	}

	public static FileSinkContentsMatcher hasContentsThat(Matcher<String> matcher) {
		return new FileSink.FileSinkContentsMatcher(matcher);
	}

	public static FileSinkTrimmedContentsMatcher hasTrimmedContentsThat(Matcher<String> matcher) {
		return new FileSink.FileSinkTrimmedContentsMatcher(matcher);
	}

	public static FileContentsMatcher fileContent(Matcher<String> matcher) {
		return new FileContentsMatcher(matcher);
	}


	/**
	 * A Matcher for File content.
	 */
	private static final class FileContentsMatcher extends DiagnosingMatcher<File> {

		private final Matcher<String> matcher;

		public FileContentsMatcher(Matcher<String> matcher) {
			this.matcher = matcher;
		}

		@Override
		public void describeTo(Description description) {
			description.appendDescriptionOf(matcher);
		}

		@Override
		protected boolean matches(Object item, Description mismatchDescription) {
			File file = (File) item;
			try {
				String contents = FileCopyUtils.copyToString(new FileReader(file));
				mismatchDescription.appendValue(contents);
				return matcher.matches(contents);
			}
			catch (IOException e) {
				mismatchDescription.appendText("failed with an IOException: " + e.getMessage());
				return false;
			}
		}
	}
}
