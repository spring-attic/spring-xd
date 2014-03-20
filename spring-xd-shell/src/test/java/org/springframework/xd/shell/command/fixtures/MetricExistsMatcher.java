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

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

import org.springframework.xd.shell.util.Table;
import org.springframework.xd.shell.util.TableRow;

/**
 * A matcher that will assert that the {@code <metric type> list} shell command returns a list that contains the
 * expected metric.
 * 
 * @param <T> the type of metric sink fixture this is matching
 * @author Eric Bottard
 */
public class MetricExistsMatcher<T extends AbstractMetricSink> extends BaseMatcher<T> {

	@Override
	public boolean matches(Object item) {
		AbstractMetricSink metric = (AbstractMetricSink) item;
		Table table = (Table) metric.shell.executeCommand(metric.dslName + " list").getResult();
		return table.getRows().contains(new TableRow().addValue(1, metric.name));
	}

	@Override
	public void describeTo(Description description) {
		description.appendText("an existing metric");
	}

	@Override
	public void describeMismatch(Object item, Description description) {
		AbstractMetricSink sink = (AbstractMetricSink) item;
		description.appendText(sink.dslName).appendText(" named '").appendText(sink.name).appendText(
				"' did not exist");
	}

}
