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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.PagedResources;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;
import org.springframework.xd.rest.client.RichGaugeOperations;
import org.springframework.xd.rest.client.domain.metrics.MetricResource;
import org.springframework.xd.shell.XDShell;
import org.springframework.xd.shell.util.Table;

/**
 * Commands for interacting with RichGauge analytics.
 *
 * @author Ilayaperumal Gopinathan
 */
@Component
public class RichGaugeCommands extends AbstractMetricsCommands implements CommandMarker {

	protected RichGaugeCommands() {
		super("RichGauge");
	}

	private static final String DISPLAY_RICH_GAUGE = "richgauge display";

	private static final String LIST_RICH_GAUGES = "richgauge list";

	private static final String DELETE_RICH_GAUGE = "richgauge delete";

	@Autowired
	private XDShell xdShell;

	@CliAvailabilityIndicator({ LIST_RICH_GAUGES, DELETE_RICH_GAUGE})
	public boolean available() {
		return xdShell.getSpringXDOperations() != null;
	}

	@CliCommand(value = LIST_RICH_GAUGES, help = "List all available richgauge names")
	public Table list(/* TODO */) {
		PagedResources<MetricResource> list = richGaugeOperations().list(/* TODO */);
		return displayMetrics(list);
	}
	
	@CliCommand(value = DISPLAY_RICH_GAUGE, help = "Display Rich Gauge value")
	public String display(
			@CliOption(key = {"", "name"}, help = "the name of the richgauge to display value", mandatory = true) String name) {
		return richGaugeOperations().display(name);
	}
	
	@CliCommand(value = DELETE_RICH_GAUGE, help= "Delete the richgauge")
	public String delete(
			@CliOption(key = {"", "name"}, help = "the name of the richgauge to delete", mandatory = true) String name) {
		richGaugeOperations().delete(name);
		return String.format("Deleted richgauge '%s'", name);
	}

	private RichGaugeOperations richGaugeOperations() {
		return xdShell.getSpringXDOperations().richGaugeOperations();
	}

}
