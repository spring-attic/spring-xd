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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.SortedMap;

import org.joda.time.DateTimeConstants;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.PagedResources;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;
import org.springframework.xd.rest.client.AggregateCounterOperations;
import org.springframework.xd.rest.client.AggregateCounterOperations.Resolution;
import org.springframework.xd.rest.domain.metrics.AggregateCountsResource;
import org.springframework.xd.rest.domain.metrics.MetricResource;
import org.springframework.xd.shell.XDShell;
import org.springframework.xd.shell.converter.NumberFormatConverter;
import org.springframework.xd.shell.util.Assertions;
import org.springframework.xd.shell.util.Table;
import org.springframework.xd.shell.util.TableHeader;

/**
 * Commands for interacting with aggregate counter analytics.
 *
 * @author Ilayaperumal Gopinathan
 */
@Component
public class AggregateCounterCommands extends AbstractMetricsCommands implements CommandMarker {

	protected AggregateCounterCommands() {
		super("AggregateCounter");
	}

	private static final String DISPLAY_AGGR_COUNTER = "aggregate-counter display";

	private static final String LIST_AGGR_COUNTERS = "aggregate-counter list";

	private static final String DELETE_AGGR_COUNTER = "aggregate-counter delete";

	@Autowired
	private XDShell xdShell;

	@CliAvailabilityIndicator({ DISPLAY_AGGR_COUNTER, LIST_AGGR_COUNTERS, DELETE_AGGR_COUNTER })
	public boolean available() {
		return xdShell.getSpringXDOperations() != null;
	}

	@CliCommand(value = DISPLAY_AGGR_COUNTER, help = "Display aggregate counter values by chosen interval and resolution(minute, hour)")
	public Table display(
			@CliOption(key = { "", "name" }, help = "the name of the aggregate counter to display", mandatory = true, optionContext = "existing-aggregate-counter disable-string-converter") String name,
			@CliOption(key = "from", help = "start-time for the interval. format: 'yyyy-MM-dd HH:mm:ss'", mandatory = false) String from,
			@CliOption(key = "to", help = "end-time for the interval. format: 'yyyy-MM-dd HH:mm:ss'. defaults to now", mandatory = false) String to,
			@CliOption(key = "lastHours", help = "set the interval to last 'n' hours", mandatory = false) Integer lastHours,
			@CliOption(key = "lastDays", help = "set the interval to last 'n' days", mandatory = false) Integer lastDays,
			@CliOption(key = "resolution", help = "the size of the bucket to aggregate (minute, hour, day, month)", mandatory = false, unspecifiedDefaultValue = "hour") Resolution resolution,
			@CliOption(key = "pattern", help = "the pattern used to format the count values (see DecimalFormat)", mandatory = false, unspecifiedDefaultValue = NumberFormatConverter.DEFAULT) NumberFormat pattern) {

		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		try {
			Date fromDate;
			switch (Assertions.atMostOneOf("from", from, "lastHours", lastHours, "lastDays", lastDays)) {
				case 0:
					fromDate = dateFormat.parse(from);
					break;
				case 1:
					fromDate = new Date(System.currentTimeMillis() - ((long) lastHours)
							* DateTimeConstants.MILLIS_PER_HOUR);
					break;
				case 2:
					fromDate = new Date(System.currentTimeMillis() - ((long) lastDays)
							* DateTimeConstants.MILLIS_PER_DAY);
					break;
				default:
					fromDate = null;
					break;
			}

			Date toDate = (to == null) ? null : dateFormat.parse(to);
			AggregateCountsResource aggResource = aggrCounterOperations().retrieve(name, fromDate, toDate, resolution);
			return displayAggrCounter(aggResource, pattern);
		}
		catch (ParseException pe) {
			throw new IllegalArgumentException(
					"Parse exception ocurred while parsing the 'from/to' options. The accepted date format is "
							+ dateFormat.toPattern());
		}
	}

	@CliCommand(value = LIST_AGGR_COUNTERS, help = "List all available aggregate counter names")
	public Table list(/* TODO */) {
		PagedResources<MetricResource> list = aggrCounterOperations().list(/* TODO */);
		return displayMetrics(list);
	}

	@CliCommand(value = DELETE_AGGR_COUNTER, help = "Delete an aggregate counter")
	public String delete(
			@CliOption(key = { "", "name" }, help = "the name of the aggregate counter to delete", mandatory = true, optionContext = "existing-aggregate-counter disable-string-converter") String name) {
		aggrCounterOperations().delete(name);
		return String.format("Deleted aggregatecounter '%s'", name);
	}

	private AggregateCounterOperations aggrCounterOperations() {
		return xdShell.getSpringXDOperations().aggrCounterOperations();
	}

	private Table displayAggrCounter(AggregateCountsResource aggResource, NumberFormat pattern) {
		final SortedMap<Date, Long> values = aggResource.getValues();
		Table t = new Table();
		t.addHeader(1, new TableHeader("AggregateCounter=" + aggResource.getName())).addHeader(2, new TableHeader(""))
				.addHeader(3, new TableHeader(""));
		t.newRow().addValue(1, "TIME").addValue(2, "-").addValue(3, "COUNT");
		for (Map.Entry<Date, Long> entry : values.entrySet()) {
			t.newRow().addValue(1, entry.getKey().toString()).addValue(2, "|")
					.addValue(3, pattern.format(entry.getValue()));
		}
		return t;
	}

}
