/*
 * Copyright 2013-2014 the original author or authors.
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

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.PagedResources;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;
import org.springframework.xd.rest.client.RuntimeOperations;
import org.springframework.xd.rest.client.domain.ContainerAttributesResource;
import org.springframework.xd.rest.client.domain.ModuleMetadataResource;
import org.springframework.xd.shell.XDShell;
import org.springframework.xd.shell.util.Table;
import org.springframework.xd.shell.util.TableHeader;
import org.springframework.xd.shell.util.TableRow;

/**
 * Commands to interact with runtime containers/modules.
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
		final PagedResources<ContainerAttributesResource> containers = runtimeOperations().listRuntimeContainers();
		final Table table = new Table();
		table.addHeader(1, new TableHeader("Container Id"))
				.addHeader(2, new TableHeader("Host"))
				.addHeader(3, new TableHeader("IP Address"))
				.addHeader(4, new TableHeader("PID"))
				.addHeader(5, new TableHeader("Groups"))
				.addHeader(6, new TableHeader("Custom Attributes"));
		for (ContainerAttributesResource container : containers) {
			Map<String, String> copy = new HashMap<String, String>(container.getAttributes());
			final TableRow row = table.newRow();
			row.addValue(1, copy.remove("id"))
					.addValue(2, copy.remove("host"))
					.addValue(3, copy.remove("ip"))
					.addValue(4, copy.remove("pid"));
			String groups = copy.remove("groups");
			row.addValue(5, groups == null ? "" : groups);
			row.addValue(6, copy.isEmpty() ? "" : copy.toString());
		}
		return table;
	}

	@CliCommand(value = LIST_MODULES, help = "List runtime modules")
	public Table listDeployedModules(
			@CliOption(mandatory = false, key = { "", "containerId" }, help = "to filter by container id") String containerId) {

		Iterable<ModuleMetadataResource> runtimeModules;
		if (containerId != null) {
			runtimeModules = runtimeOperations().listRuntimeModulesByContainer(containerId);
		}
		else {
			runtimeModules = runtimeOperations().listRuntimeModules();
		}
		final Table table = new Table();
		table.addHeader(1, new TableHeader("Module")).addHeader(2, new TableHeader("Container Id")).addHeader(
				3, new TableHeader("Options"));
		for (ModuleMetadataResource module : runtimeModules) {
			final TableRow row = table.newRow();
			row.addValue(1, module.getModuleId()).addValue(2, module.getContainerId()).addValue(3,
					module.getProperties());
		}
		return table;
	}

	private RuntimeOperations runtimeOperations() {
		return xdShell.getSpringXDOperations().runtimeOperations();
	}

}
