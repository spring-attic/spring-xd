/*
 * Copyright 2002-2013 the original author or authors.
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

import java.text.NumberFormat;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.PagedResources;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;
import org.springframework.xd.rest.client.FieldValueCounterOperations;
import org.springframework.xd.rest.client.domain.metrics.FieldValueCounterResource;
import org.springframework.xd.rest.client.domain.metrics.MetricResource;
import org.springframework.xd.shell.XDShell;
import org.springframework.xd.shell.converter.NumberFormatConverter;
import org.springframework.xd.shell.util.Table;
import org.springframework.xd.shell.util.TableHeader;

/**
 * Commands for interacting with field value counter analytics.
 * 
 * @author Ilayaperumal Gopinathan
 */
@Component
public class FieldValueCounterCommands extends AbstractMetricsCommands implements CommandMarker {

	protected FieldValueCounterCommands() {
		super("FieldValueCounter");
	}

	private static final String DISPLAY_FV_COUNTER = "fieldvaluecounter display";

	private static final String LIST_FV_COUNTERS = "fieldvaluecounter list";

	private static final String DELETE_FV_COUNTER = "fieldvaluecounter delete";

	@Autowired
	private XDShell xdShell;

	@CliAvailabilityIndicator({ DISPLAY_FV_COUNTER, LIST_FV_COUNTERS, DELETE_FV_COUNTER })
	public boolean available() {
		return xdShell.getSpringXDOperations() != null;
	}

	@CliCommand(value = DISPLAY_FV_COUNTER, help = "Display the value of a field-value-counter")
	public Table display(
			@CliOption(key = { "", "name" }, help = "the name of the field-value-counter to display", mandatory = true, optionContext = "existing-fvc disable-string-converter") String name,
			@CliOption(key = "pattern", help = "the pattern used to format the field-value-counter's field count (see DecimalFormat)", mandatory = false, unspecifiedDefaultValue = NumberFormatConverter.DEFAULT) NumberFormat pattern,
			@CliOption(key = { "size" }, help = "the number of values to display", mandatory = false, unspecifiedDefaultValue = "25") int size) {
		FieldValueCounterResource fvcResource = fvcOperations().retrieve(name);
		return displayFVCvalue(fvcResource, pattern, size);
	}

	@CliCommand(value = LIST_FV_COUNTERS, help = "List all available field-value-counter names")
	public Table list(/* TODO */) {
		PagedResources<MetricResource> list = fvcOperations().list(/* TODO */);
		return displayMetrics(list);
	}

	@CliCommand(value = DELETE_FV_COUNTER, help = "Delete the field-value-counter with the given name")
	public String delete(
			@CliOption(key = { "", "name" }, help = "the name of the field-value-counter to delete", mandatory = true, optionContext = "existing-fvc disable-string-converter") String name) {
		fvcOperations().delete(name);
		return String.format("Deleted field-value-counter '%s'", name);
	}

	private FieldValueCounterOperations fvcOperations() {
		return xdShell.getSpringXDOperations().fvcOperations();
	}

	private Table displayFVCvalue(FieldValueCounterResource fvcResource, NumberFormat pattern, final int size) {
		final Map<String, Double> fieldValueCounts = fvcResource.getFieldValueCounts();
		FieldValueComparator fvc = new FieldValueComparator(fieldValueCounts);
		TreeMap<String, Double> sortedFvc = new TreeMap<String, Double>(fvc);
		sortedFvc.putAll(fieldValueCounts);
		Table t = new Table();
		t.addHeader(1, new TableHeader("FieldName=" + fvcResource.getName())).addHeader(2, new TableHeader(""))
				.addHeader(3, new TableHeader(""));
		t.newRow().addValue(1, "VALUE").addValue(2, "-").addValue(3, "COUNT");
		int rowSize = 1;
		for (Map.Entry<String, Double> entry : sortedFvc.entrySet()) {
			t.newRow().addValue(1, entry.getKey()).addValue(2, "|").addValue(3, pattern.format(entry.getValue()));
			if (rowSize >= size) {
				break;
			}
			rowSize++;
		}
		return t;
	}

	private class FieldValueComparator implements Comparator<String> {

		Map<String, Double> fieldValueCounts;

		public FieldValueComparator(Map<String, Double> fieldValueCounts) {
			this.fieldValueCounts = fieldValueCounts;
		}

		@Override
		public int compare(String a, String b) {
			if (fieldValueCounts.get(a) > fieldValueCounts.get(b)) {
				return -1;
			}
			if (fieldValueCounts.get(a).equals(fieldValueCounts.get(b))) {
				return a.compareTo(b);
			}
			else {
				return 1;
			}
		}

	}
}
