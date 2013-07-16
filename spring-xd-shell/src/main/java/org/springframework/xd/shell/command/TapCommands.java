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

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;
import org.springframework.xd.rest.client.TapOperations;
import org.springframework.xd.rest.client.domain.TapDefinitionResource;
import org.springframework.xd.shell.XDShell;
import org.springframework.xd.shell.util.Table;
import org.springframework.xd.shell.util.TableHeader;
import org.springframework.xd.shell.util.TableRow;
import org.springframework.xd.shell.util.UiUtils;

/**
 * Tap commands.
 * 
 * @author Ilayaperumal Gopinathan
 * @author Gunnar Hillert
 * 
 * @since 1.0
 */

@Component
public class TapCommands implements CommandMarker {

	private final static String CREATE_TAP = "tap create";

	private final static String LIST_TAPS = "tap list";

	@Autowired
	private XDShell xdShell;

	@CliAvailabilityIndicator({ CREATE_TAP, LIST_TAPS })
	public boolean available() {
		return xdShell.getSpringXDOperations() != null;
	}

	@CliCommand(value = CREATE_TAP, help = "Create a tap")
	public String createTap(
			@CliOption(mandatory = true, key = "name", help = "the name to give to the tap")
			String name,
			@CliOption(mandatory = true, key = { "", "definition" }, help = "tap definition, using XD DSL (e.g. \"tap@mystream.filter | sink1\")")
			String dsl,
			@CliOption(key = "deploy", help = "whether to deploy the tap immediately", unspecifiedDefaultValue = "true")
			boolean autoStart) {
		try {
			tapOperations().createTap(name, dsl, autoStart);
		}
		catch (Exception e) {
			return String.format("Error creating tap '%s'", name);
		}
		return String.format((autoStart ? "Successfully created and deployed tap '%s'"
				: "Successfully created tap '%s'"), name);
	}

	@CliCommand(value = LIST_TAPS, help = "List all taps")
	public String listTaps() {

		final List<TapDefinitionResource> taps;

		try {
			taps = tapOperations().listTaps();
		}
		catch (Exception e) {
			return String.format("Error listing taps");
		}

		final Table table = new Table();
		table.addHeader(1, new TableHeader("Tap Name")).addHeader(2, new TableHeader("Stream Name"))
				.addHeader(3, new TableHeader("Tap Definition"));

		for (TapDefinitionResource tapDefinitionResource : taps) {
			final TableRow row = new TableRow();
			row.addValue(1, tapDefinitionResource.getName()).addValue(2, tapDefinitionResource.getStreamName())
					.addValue(3, tapDefinitionResource.getDefinition());
			table.getRows().add(row);
		}

		return UiUtils.renderTextTable(table);
	}

	private TapOperations tapOperations() {
		return xdShell.getSpringXDOperations().tapOperations();
	}

}
