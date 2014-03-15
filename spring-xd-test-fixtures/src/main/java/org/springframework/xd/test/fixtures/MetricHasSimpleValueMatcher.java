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

import org.hamcrest.Description;
import org.hamcrest.DiagnosingMatcher;

/**
 * A matcher that will assert that the {@code <metric type> display xxx} shell command displays the expected value.
 * 
 * @param <U> the type of the value returned by the display command
 * @param <T> the type of metric sink fixture this is matching
 * @author Eric Bottard
 */
public class MetricHasSimpleValueMatcher<U, T extends AbstractMetricSink & HasDisplayValue<U>> extends
		DiagnosingMatcher<T> {

	private final U expectedValue;

	public MetricHasSimpleValueMatcher(U expectedValue) {
		this.expectedValue = expectedValue;
	}

	@Override
	protected boolean matches(Object item, Description mismatchDescription) {
		U actualValue = actualValue(item);
		mismatchDescription.appendText("was ").appendValue(actualValue);
		return expectedValue.equals(actualValue);
	}

	private U actualValue(Object item) {
		AbstractMetricSink metric = (AbstractMetricSink) item;
		@SuppressWarnings("unchecked")
		U value = (U) metric.shell.executeCommand(metric.getDslName() + " display " + metric.getName()).getResult();
		return value;
	}

	@Override
	public void describeTo(Description description) {
		description.appendValue(expectedValue);
	}

}
