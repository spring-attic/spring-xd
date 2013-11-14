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
import org.springframework.xd.rest.client.ModuleOperations;
import org.springframework.xd.rest.client.domain.ModuleDefinitionResource;
import org.springframework.xd.rest.client.domain.RESTModuleType;
import org.springframework.xd.shell.XDShell;
import org.springframework.xd.shell.util.Table;
import org.springframework.xd.shell.util.TableHeader;
import org.springframework.xd.shell.util.TableRow;
import org.springframework.xd.shell.util.UiUtils;

/**
 * Module commands.
 * 
 * @author Glenn Renfro
 * 
 */

@Component
public class ModuleCommands implements CommandMarker {

	private final static String COMPOSE_MODULE = "module compose";

	private final static String DISPLAY_MODULE = "module display";

	private final static String LIST_MODULES = "module list";

	private final static String DELETE_MODULE = "module delete";


	@Autowired
	private XDShell xdShell;

	@CliAvailabilityIndicator({ COMPOSE_MODULE, LIST_MODULES })
	public boolean available() {
		return xdShell.getSpringXDOperations() != null;
	}

	@CliCommand(value = COMPOSE_MODULE, help = "Create a virtual module")
	public String createModule(
			@CliOption(mandatory = true, key = { "name", "" }, help = "the name to give to the module") String name,
			@CliOption(mandatory = true, key = "definition", help = "module definition using xd dsl") String dsl) {
		ModuleDefinitionResource composedModule = moduleOperations().composeModule(name, dsl);
		return String.format(("Successfully created module '%s' with type %s"), composedModule.getName(),
				composedModule.getType());
	}

	@CliCommand(value = DELETE_MODULE, help = "Delete a virtual module")
	public String destroyModule(
			// TODO: replace with converter once XD-953b is merged in
			@CliOption(mandatory = true, key = { "", "name" }, help = "the name of the the module") String name,
			@CliOption(mandatory = true, key = "type", help = "the type of the module") RESTModuleType moduleType) {
		moduleOperations().deleteModule(name, moduleType);
		return String.format(("Successfully destroyed module '%s' with type %s"), name,
				moduleType);
	}

	@CliCommand(value = DISPLAY_MODULE, help = "Display the configuration file of a module")
	public String display(
			// TODO: replace with converter once XD-953b is merged in
			@CliOption(mandatory = true, key = { "", "name" }, help = "the name of the the module") String name,
			@CliOption(mandatory = true, key = "type", help = "the type of the module") RESTModuleType moduleType) {

		final String configurationFileContents = moduleOperations().downloadConfigurationFile(
				moduleType, name);

		final StringBuilder sb = new StringBuilder()
				.append(String.format("Configuration file contents for module definiton '%s' (%s):%n%n", name,
						moduleType))
				.append(UiUtils.HORIZONTAL_LINE)
				.append(configurationFileContents)
				.append(UiUtils.HORIZONTAL_LINE);

		return sb.toString();

	}

	@CliCommand(value = LIST_MODULES, help = "List all modules")
	public Table listModules(
			@CliOption(key = "type", help = "retrieve a specific type of module") RESTModuleType moduleType) {
		PagedResources<ModuleDefinitionResource> modules = null;
		modules = moduleOperations().list(moduleType);
		final Table table = new Table();
		table.addHeader(1, new TableHeader("Module Name")).addHeader(2, new TableHeader("Module Type"));

		for (ModuleDefinitionResource moduleDefinitionResource : modules) {
			final TableRow row = new TableRow();
			row.addValue(1, moduleDefinitionResource.getName()).addValue(2, moduleDefinitionResource.getType());
			table.getRows().add(row);
		}
		return table;
	}

	private ModuleOperations moduleOperations() {
		return xdShell.getSpringXDOperations().moduleOperations();
	}
}
