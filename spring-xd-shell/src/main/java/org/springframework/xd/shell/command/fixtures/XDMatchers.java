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

package org.springframework.xd.shell.command.fixtures;

import org.hamcrest.Matcher;
import org.springframework.xd.shell.command.fixtures.FileSink.FileSinkContentsMatcher;


/**
 * Utility class that provides static factory methods for {@link Matcher}s in this package.
 * 
 * @author Eric Bottard
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

}
