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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.PagedResources;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;
import org.springframework.xd.rest.client.CounterOperations;
import org.springframework.xd.rest.domain.metrics.CounterResource;
import org.springframework.xd.rest.domain.metrics.MetricResource;
import org.springframework.xd.shell.XDShell;
import org.springframework.xd.shell.converter.NumberFormatConverter;
import org.springframework.xd.shell.util.Table;

/**
 * Commands for interacting with Counter analytics.
 * 
 * @author Eric Bottard
 * @author Ilayaperumal Gopinathan
 */
@Component
public class CounterCommands extends AbstractMetricsCommands implements CommandMarker {

	protected CounterCommands() {
		super("Counter");
	}

	private static final String DISPLAY_COUNTER = "counter display";

	private static final String LIST_COUNTERS = "counter list";

	private static final String DELETE_COUNTER = "counter delete";

	@Autowired
	private XDShell xdShell;

	@CliAvailabilityIndicator({ LIST_COUNTERS, DISPLAY_COUNTER, DELETE_COUNTER })
	public boolean available() {
		return xdShell.getSpringXDOperations() != null;
	}

	@CliCommand(value = DISPLAY_COUNTER, help = "Display the value of a counter")
	public String display(
			@CliOption(key = { "", "name" }, help = "the name of the counter to display", mandatory = true, optionContext = "existing-counter disable-string-converter") String name,
			@CliOption(key = "pattern", help = "the pattern used to format the value (see DecimalFormat)", mandatory = false, unspecifiedDefaultValue = NumberFormatConverter.DEFAULT) NumberFormat pattern) {
		CounterResource counter = counterOperations().retrieve(name);

		return pattern.format(counter.getValue());
	}

	@CliCommand(value = LIST_COUNTERS, help = "List all available counter names")
	public Table list(/* TODO */) {
		PagedResources<MetricResource> list = counterOperations().list(/* TODO */);
		return displayMetrics(list);
	}

	@CliCommand(value = DELETE_COUNTER, help = "Delete the counter with the given name")
	public String delete(
			@CliOption(mandatory = true, key = { "", "name" }, help = "the name of the counter to delete", optionContext = "existing-counter disable-string-converter") String name) {
		counterOperations().delete(name);
		return String.format("Deleted counter '%s'", name);
	}

	private CounterOperations counterOperations() {
		return xdShell.getSpringXDOperations().counterOperations();
	}

}
