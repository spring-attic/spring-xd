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
import org.springframework.xd.rest.client.GaugeOperations;
import org.springframework.xd.rest.domain.metrics.GaugeResource;
import org.springframework.xd.rest.domain.metrics.MetricResource;
import org.springframework.xd.shell.XDShell;
import org.springframework.xd.shell.converter.NumberFormatConverter;
import org.springframework.xd.shell.util.Table;

/**
 * Commands for interacting with Gauge analytics.
 * 
 * @author Ilayaperumal Gopinathan
 */
@Component
public class GaugeCommands extends AbstractMetricsCommands implements CommandMarker {

	protected GaugeCommands() {
		super("Gauge");
	}

	private static final String DISPLAY_GAUGE = "gauge display";

	private static final String LIST_GAUGES = "gauge list";

	private static final String DELETE_GAUGE = "gauge delete";

	@Autowired
	private XDShell xdShell;

	@CliAvailabilityIndicator({ LIST_GAUGES, DISPLAY_GAUGE, DELETE_GAUGE })
	public boolean available() {
		return xdShell.getSpringXDOperations() != null;
	}

	@CliCommand(value = LIST_GAUGES, help = "List all available gauge names")
	public Table list(/* TODO */) {
		PagedResources<MetricResource> list = gaugeOperations().list(/* TODO */);
		return displayMetrics(list);
	}

	@CliCommand(value = DISPLAY_GAUGE, help = "Display the value of a gauge")
	public String display(
			@CliOption(key = { "", "name" }, help = "the name of the gauge to display", mandatory = true, optionContext = "existing-gauge disable-string-converter") String name,
			@CliOption(key = "pattern", help = "the pattern used to format the value (see DecimalFormat)", mandatory = false, unspecifiedDefaultValue = NumberFormatConverter.DEFAULT) NumberFormat pattern) {
		GaugeResource gauge = gaugeOperations().retrieve(name);
		return pattern.format(gauge.getValue());
	}

	@CliCommand(value = DELETE_GAUGE, help = "Delete a gauge")
	public String delete(
			@CliOption(key = { "", "name" }, help = "the name of the gauge to delete", mandatory = true, optionContext = "existing-gauge disable-string-converter") String name) {
		gaugeOperations().delete(name);
		return String.format("Deleted gauge '%s'", name);
	}

	private GaugeOperations gaugeOperations() {
		return xdShell.getSpringXDOperations().gaugeOperations();
	}

}
