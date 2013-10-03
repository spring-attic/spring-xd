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

package org.springframework.xd.shell.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.PagedResources;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;
import org.springframework.xd.rest.client.ContainerOperations;
import org.springframework.xd.rest.client.RuntimeModulesOperations;
import org.springframework.xd.rest.client.domain.ContainerResource;
import org.springframework.xd.rest.client.domain.ModuleResource;
import org.springframework.xd.shell.XDShell;
import org.springframework.xd.shell.util.Table;
import org.springframework.xd.shell.util.TableHeader;
import org.springframework.xd.shell.util.TableRow;


/**
 * Commands to interact with runtime containers/deployed modules
 * 
 * @author Ilayaperumal Gopinathan
 */
@Component
public class RuntimeCommands implements CommandMarker {

	private static final String LIST_CONTAINERS = "runtime containers";

	private static final String LIST_MODULES = "runtime modules";

	@Autowired
	private XDShell xdShell;

	@CliAvailabilityIndicator({ LIST_CONTAINERS, LIST_MODULES })
	public boolean available() {
		return xdShell.getSpringXDOperations() != null;
	}

	@CliCommand(value = LIST_CONTAINERS, help = "List runtime containers")
	public Table listContainers() {

		final PagedResources<ContainerResource> containers = containerOperations().list();

		final Table table = new Table();
		table.addHeader(1, new TableHeader("Container Id")).addHeader(2, new TableHeader("Host")).addHeader(
				3, new TableHeader("IP Address"));
		for (ContainerResource container : containers) {
			final TableRow row = table.newRow();
			row.addValue(1, container.getContainerId()).addValue(2, container.getHostName()).addValue(3,
					container.getIpAddress());
		}
		return table;
	}

	@CliCommand(value = LIST_MODULES, help = "List runtime modules")
	public Table listDeployedModules(
			@CliOption(mandatory = false, key = { "", "containerId" }, help = "to filter by container id") String containerId) {

		Iterable<ModuleResource> runtimeModules;
		if (containerId != null) {
			runtimeModules = moduleOperations().listByContainer(containerId);
		}
		else {
			runtimeModules = moduleOperations().list();
		}
		final Table table = new Table();
		table.addHeader(1, new TableHeader("Container Id")).addHeader(2, new TableHeader("Group")).addHeader(
				3, new TableHeader("Index")).addHeader(4, new TableHeader("Properties"));
		for (ModuleResource module : runtimeModules) {
			final TableRow row = table.newRow();
			row.addValue(1, module.getContainerId()).addValue(2, module.getGroup()).addValue(3,
					module.getIndex()).addValue(4, module.getProperties());
		}
		return table;
	}

	private RuntimeModulesOperations moduleOperations() {
		return xdShell.getSpringXDOperations().runtimeModulesOperations();
	}

	private ContainerOperations containerOperations() {
		return xdShell.getSpringXDOperations().containerOperations();
	}

}
