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
import org.springframework.xd.shell.util.Assertions;
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

	private static final String LIST_TRIGGERS = "triggers list";

	private final static String DEPLOY_TRIGGER = "trigger deploy";

	private final static String UNDEPLOY_TRIGGER = "trigger undeploy";

	private static final String DESTROY_TRIGGER = "trigger destroy";

	@Autowired
	private XDShell xdShell;

	@CliAvailabilityIndicator({ CREATE_TRIGGER, LIST_TRIGGERS, DEPLOY_TRIGGER, UNDEPLOY_TRIGGER, DESTROY_TRIGGER })
	public boolean available() {
		return xdShell.getSpringXDOperations() != null;
	}

	@CliCommand(value = CREATE_TRIGGER, help = "Create a new trigger with a given cron expression")
	public String createTrigger(
			@CliOption(mandatory = true, key = { "", "name" }, help = "the name to give to the trigger") String name,
			@CliOption(mandatory = true, key = { "definition" }, help = "definition for the trigger") String definition) {
		triggerOperations().createTrigger(name, definition);
		return String.format("Created new trigger '%s'", name);
	}

	@CliCommand(value = LIST_TRIGGERS, help = "List all triggers")
	public Table listTriggers() {

		final PagedResources<TriggerDefinitionResource> triggers = triggerOperations().list();

		final Table table = new Table();
		table.addHeader(1, new TableHeader("Trigger Name")).addHeader(2, new TableHeader("Trigger Definition"));

		for (TriggerDefinitionResource triggerDefinitionResource : triggers.getContent()) {
			final TableRow row = new TableRow();
			row.addValue(1, triggerDefinitionResource.getName()).addValue(2, triggerDefinitionResource.getDefinition());
			table.getRows().add(row);
		}

		return table;
	}

	@CliCommand(value = DESTROY_TRIGGER, help = "Destroy existing trigger(s)")
	public String destroyTrigger(
			@CliOption(key = { "", "name" }, help = "the name of the trigger to destroy", optionContext = "existing-trigger disable-string-converter") String name,
			@CliOption(key = { "all" }, help = "destroy all the existing triggers", unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean all) {
		String message = "";
		switch (Assertions.exactlyOneOf("name", name, "all", all)) {
			case 0:
				triggerOperations().destroy(name);
				message = String.format("Destroyed trigger '%s'", name);
				break;
			case 1:
				triggerOperations().destroyAll();
				message = String.format("Destroyed all the triggers");
				break;
			default:
				throw new IllegalArgumentException("You must specify exactly one of 'name', 'all'");
		}
		return message;
	}

	@CliCommand(value = DEPLOY_TRIGGER, help = "Deploy existing trigger(s)")
	public String deployTrigger(
			@CliOption(key = { "", "name" }, help = "the name of the trigger to deploy", optionContext = "existing-trigger disable-string-converter") String name,
			@CliOption(key = { "all" }, help = "deploy all the existing triggers", unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean all) {
		String message = "";
		switch (Assertions.exactlyOneOf("name", name, "all", all)) {
			case 0:
				triggerOperations().deploy(name);
				message = String.format("Deployed trigger '%s'", name);
				break;
			case 1:
				triggerOperations().deployAll();
				message = String.format("Deployed all the triggers");
				break;
			default:
				throw new IllegalArgumentException("You must specify exactly one of 'name', 'all'");
		}
		return message;
	}

	@CliCommand(value = UNDEPLOY_TRIGGER, help = "Un-deploy existing trigger(s)")
	public String undeployTrigger(
			@CliOption(key = { "", "name" }, help = "the name of the trigger to un-deploy", optionContext = "existing-trigger disable-string-converter") String name,
			@CliOption(key = { "all" }, help = "undeploy all the existing triggers", unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean all) {
		String message = "";
		switch (Assertions.exactlyOneOf("name", name, "all", all)) {
			case 0:
				triggerOperations().undeploy(name);
				message = String.format("Un-deployed trigger '%s'", name);
				break;
			case 1:
				triggerOperations().undeployAll();
				message = String.format("Un-deployed all the triggers");
				break;
			default:
				throw new IllegalArgumentException("You must specify exactly one of 'name', 'all'");
		}
		return message;
	}

	private TriggerOperations triggerOperations() {
		return xdShell.getSpringXDOperations().triggerOperations();
	}
}
