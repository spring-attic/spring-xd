/*
 * Copyright 2013-2015 the original author or authors.
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

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.fusesource.jansi.Ansi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.hateoas.PagedResources;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;
import org.springframework.xd.rest.client.ModuleOperations;
import org.springframework.xd.rest.domain.DetailedModuleDefinitionResource;
import org.springframework.xd.rest.domain.DetailedModuleDefinitionResource.Option;
import org.springframework.xd.rest.domain.ModuleDefinitionResource;
import org.springframework.xd.rest.domain.RESTModuleType;
import org.springframework.xd.shell.XDShell;
import org.springframework.xd.shell.util.Table;
import org.springframework.xd.shell.util.TableHeader;
import org.springframework.xd.shell.util.TableRow;

import static org.fusesource.jansi.Ansi.ansi;

/**
 * Commands for working with modules. Allows retrieval of information about available modules, as well as creating new
 * composed modules.
 *
 * @author Glenn Renfro
 * @author Eric Bottard
 * @author Florent Biville
 * @author David Turanski
 */

@Component
public class ModuleCommands implements CommandMarker {

	private final static String COMPOSE_MODULE = "module compose";

	private final static String LIST_MODULES = "module list";

	private final static String DELETE_MODULE = "module delete";

	private final static String MODULE_INFO = "module info";

	private static final String UPLOAD_MODULE = "module upload";

	@Autowired
	private XDShell xdShell;

	@CliAvailabilityIndicator({COMPOSE_MODULE, LIST_MODULES, MODULE_INFO, DELETE_MODULE, UPLOAD_MODULE})
	public boolean available() {
		return xdShell.getSpringXDOperations() != null;
	}

	@CliCommand(value = MODULE_INFO, help = "Get information about a module")
	public String moduleInfo(
			@CliOption(mandatory = true, key = {"name", ""}, help = "name of the module to query, in the form 'type:name'") QualifiedModuleName module,
			@CliOption(key = "hidden", help = "whether to show 'hidden' options", specifiedDefaultValue = "true", unspecifiedDefaultValue = "false") boolean showHidden
	) {
		DetailedModuleDefinitionResource info = moduleOperations().info(module.name, module.type);
		List<Option> options = info.getOptions();
		StringBuilder result = new StringBuilder();
		result.append("Information about ").append(module.type.name()).append(" module '").append(module.name).append(
				"':\n\n");

		if (info.getShortDescription() != null) {
			result.append(info.getShortDescription()).append("\n\n");
		}
		if (options == null) {
			result.append("Module options metadata is not available");
		}
		else {
			Table table = new Table().addHeader(1, new TableHeader("Option Name")).addHeader(2,
					new TableHeader("Description")).addHeader(
					3, new TableHeader("Default")).addHeader(4, new TableHeader("Type"));
			for (DetailedModuleDefinitionResource.Option o : options) {
				if (!showHidden && o.isHidden()) {
					continue;
				}
				final TableRow row = new TableRow();
				row.addValue(1, o.getName())
						.addValue(2, o.getDescription())
						.addValue(3, prettyPrintDefaultValue(o))
						.addValue(4, o.getType() == null ? "<unknown>" : o.getType());
				table.getRows().add(row);
			}
			result.append(table.toString());
		}
		return result.toString();
	}

	/**
	 * Escapes some special values so that they don't disturb console rendering and are easier to read.
	 */
	private String prettyPrintDefaultValue(Option o) {
		if (o.getDefaultValue() == null) {
			return "<none>";
		}
		return o.getDefaultValue()
				.replace("\n", "\\n")
				.replace("\t", "\\t")
				.replace("\f", "\\f");
	}

	@CliCommand(value = COMPOSE_MODULE, help = "Create a virtual module")
	public String composeModule(
			@CliOption(mandatory = true, key = {"name", ""}, help = "the name to give to the module") String name,
			@CliOption(mandatory = true, key = "definition", optionContext = "completion-module disable-string-converter", help = "module definition using xd dsl") String dsl,
			@CliOption(key = "force", help = "force update if module already exists (only if not in use)", specifiedDefaultValue = "true", unspecifiedDefaultValue = "false") boolean force
	) {
		ModuleDefinitionResource composedModule = moduleOperations().composeModule(name, dsl, force);
		return String.format(("Successfully created module '%s' with type %s"), composedModule.getName(),
				composedModule.getType());
	}

	@CliCommand(value = UPLOAD_MODULE, help = "Upload a new module")
	public String uploadModule(
			@CliOption(mandatory = true, key = {"type"}, help = "the type for the uploaded module") RESTModuleType type,
			@CliOption(mandatory = true, key = {"name"}, help = "the name for the uploaded module") String name,
			@CliOption(mandatory = true, key = {"", "file"}, help = "path to the module archive") File file,
			@CliOption(key = "force", help = "force update if module already exists (only if not in use)", specifiedDefaultValue = "true", unspecifiedDefaultValue = "false") boolean force
	) throws IOException {
		Resource resource = new FileSystemResource(file);
		ModuleDefinitionResource composedModule = moduleOperations().uploadModule(name, type, resource, force);
		return String.format(("Successfully uploaded module '%s:%s'"), composedModule.getType(),
				composedModule.getName());
	}

	@CliCommand(value = DELETE_MODULE, help = "Delete a virtual module")
	public String destroyModule(
			@CliOption(mandatory = true, key = {"name", ""}, help = "name of the module to delete, in the form 'type:name'") QualifiedModuleName module
	) {
		moduleOperations().deleteModule(module.name, module.type);
		return String.format(("Successfully destroyed module '%s' with type %s"), module.name,
				module.type);
	}

	@CliCommand(value = LIST_MODULES, help = "List all modules")
	public Table listModules() {
		PagedResources<ModuleDefinitionResource> modules = moduleOperations().list(null);
		return new ModuleList(modules).renderByType();
	}

	private ModuleOperations moduleOperations() {
		return xdShell.getSpringXDOperations().moduleOperations();
	}

	public static class QualifiedModuleName {

		public RESTModuleType type;

		public String name;

		public QualifiedModuleName(String name, RESTModuleType type) {
			this.name = name;
			this.type = type;
		}
	}
}
