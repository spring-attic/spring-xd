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
import java.util.Map;
import java.util.TreeMap;

import org.springframework.shell.core.JLineShellComponent;
import org.springframework.xd.shell.util.Table;
import org.springframework.xd.shell.util.TableHeader;
import org.springframework.xd.test.fixtures.AbstractMetricSink;
import org.springframework.xd.test.fixtures.HasDisplayValue;

/**
 * Fixture class for an field value counter. Provides utility methods to return the expected displayed value.
 * 
 * @author Eric Bottard
 */
public class FieldValueCounterSink extends AbstractMetricSink implements HasDisplayValue<Table> {

	private String fieldName;

	public FieldValueCounterSink(JLineShellComponent shell, String name, String fieldName) {
		super(shell, name, "field-value-counter");
		this.fieldName = fieldName;
	}

	@Override
	protected String toDSL() {
		return String.format("%s --name=%s --fieldName=%s", getDslName(), getName(), fieldName);
	}

	public Table constructFVCDisplay(TreeMap<String, Double> fvcMap) {
		Table t = new Table();
		NumberFormat pattern = new DecimalFormat();
		// TODO: Should actually be fieldName=
		t.addHeader(1, new TableHeader("FieldValueCounter=" + getName())).addHeader(2, new TableHeader("")).addHeader(
				3, new TableHeader(""));
		t.newRow().addValue(1, "VALUE").addValue(2, "-").addValue(3, "COUNT");
		for (Map.Entry<String, Double> entry : fvcMap.descendingMap().entrySet()) {
			t.newRow().addValue(1, entry.getKey()).addValue(2, "|").addValue(3, pattern.format(entry.getValue()));
		}
		return t;
	}

}
