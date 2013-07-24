/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import org.springframework.xd.rest.client.TriggerOperations;
import org.springframework.xd.rest.client.domain.TriggerDefinitionResource;
import org.springframework.xd.shell.XDShell;
import org.springframework.xd.shell.util.Table;
import org.springframework.xd.shell.util.TableHeader;
import org.springframework.xd.shell.util.TableRow;

/**
 * Trigger commands.
 *
 * @author Ilayaperumal Gopinathan
 * @author Gunnar Hillert
 *
 * @since 1.0
 */

@Component
public class TriggerCommands implements CommandMarker {

	private static final String CREATE_TRIGGER = "trigger create";
	private final static String LIST_TRIGGERS  = "trigger list";
	private static final String DESTROY_TRIGGER = "trigger destroy";

	@Autowired
	private XDShell xdShell;

	@CliAvailabilityIndicator({ CREATE_TRIGGER, LIST_TRIGGERS })
	public boolean available() {
		return xdShell.getSpringXDOperations() != null;
	}

	@CliCommand(value = CREATE_TRIGGER, help = "Create a new trigger with a given cron expression")
	public String createTrigger(
			@CliOption(mandatory = true, key = { "", "name" }, help = "the name to give to the trigger")
			String name,
			@CliOption(mandatory = true, key = { "definition" }, help = "definition for the trigger")
			String definition) {
		triggerOperations().createTrigger(name, definition);
		return String.format("Created new trigger '%s'", name);
	}

	@CliCommand(value = LIST_TRIGGERS, help = "List all triggers")
	public Table listTriggers() {

		final PagedResources<TriggerDefinitionResource> triggers = triggerOperations().listTriggers();

		final Table table = new Table();
		table.addHeader(1, new TableHeader("Trigger Name")).addHeader(2, new TableHeader("Trigger Definition"));

		for (TriggerDefinitionResource triggerDefinitionResource : triggers.getContent()) {
			final TableRow row = new TableRow();
			row.addValue(1, triggerDefinitionResource.getName()).addValue(2, triggerDefinitionResource.getDefinition());
			table.getRows().add(row);
		}

		return table;
	}

	@CliCommand(value = DESTROY_TRIGGER, help = "Destroy an existing trigger")
	public String destroyTrigger(
			@CliOption(mandatory = true, key = { "", "name" }, help = "the name of the trigger to be destroyed") String name) {
		triggerOperations().deleteTrigger(name);
		return String.format("Created new trigger '%s'", name);
	}

	private TriggerOperations triggerOperations() {
		return xdShell.getSpringXDOperations().triggerOperations();
	}
}
