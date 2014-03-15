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

import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.springframework.shell.core.JLineShellComponent;
import org.springframework.xd.shell.util.Table;
import org.springframework.xd.shell.util.TableHeader;
import org.springframework.xd.test.fixtures.AbstractMetricSink;
import org.springframework.xd.test.fixtures.HasDisplayValue;

/**
 * Fixture class for a rich gauge counter. Provides utility methods to return the expected displayed value as a
 * {@link Table}.
 * 
 * @author Eric Bottard
 */

public class RichGaugeSink extends AbstractMetricSink implements HasDisplayValue<Table> {

	public RichGaugeSink(JLineShellComponent shell, String name) {
		super(shell, name, "rich-gauge");
	}

	public Table constructRichGaugeDisplay(double value, double alpha, double average, double max, double min,
			long count) {
		Table t = new Table();
		NumberFormat pattern = new DecimalFormat();
		t.addHeader(1, new TableHeader("Name")).addHeader(2, new TableHeader(getName()));
		t.newRow().addValue(1, "value").addValue(2, pattern.format(value));
		t.newRow().addValue(1, "alpha").addValue(2, pattern.format(alpha));
		t.newRow().addValue(1, "average").addValue(2, pattern.format(average));
		t.newRow().addValue(1, "max").addValue(2, pattern.format(max));
		t.newRow().addValue(1, "min").addValue(2, pattern.format(min));
		t.newRow().addValue(1, "count").addValue(2, pattern.format(count));
		return t;
	}
}
