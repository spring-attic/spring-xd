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
import org.springframework.xd.rest.client.TapOperations;
import org.springframework.xd.rest.client.domain.TapDefinitionResource;
import org.springframework.xd.shell.XDShell;
import org.springframework.xd.shell.util.Assertions;
import org.springframework.xd.shell.util.Table;
import org.springframework.xd.shell.util.TableHeader;
import org.springframework.xd.shell.util.TableRow;

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

	private final static String LIST_TAPS = "taps list";

	private final static String DEPLOY_TAP = "tap deploy";

	private final static String UNDEPLOY_TAP = "tap undeploy";

	private final static String DESTROY_TAP = "tap destroy";

	@Autowired
	private XDShell xdShell;

	@CliAvailabilityIndicator({ CREATE_TAP, LIST_TAPS, DEPLOY_TAP, UNDEPLOY_TAP, DESTROY_TAP })
	public boolean available() {
		return xdShell.getSpringXDOperations() != null;
	}

	@CliCommand(value = CREATE_TAP, help = "Create a tap")
	public String createTap(
			@CliOption(mandatory = true, key = { "", "name" }, help = "the name to give to the tap") String name,
			@CliOption(mandatory = true, key = "definition", help = "tap definition, using XD DSL (e.g. \"tap@mystream.filter | sink1\")") String dsl,
			@CliOption(key = "deploy", help = "whether to deploy the tap immediately", unspecifiedDefaultValue = "true") boolean autoStart) {
		tapOperations().createTap(name, dsl, autoStart);
		return String.format((autoStart ? "Created and deployed new tap '%s'" : "Created new tap '%s'"), name);
	}

	@CliCommand(value = LIST_TAPS, help = "List all taps")
	public Table listTaps() {

		final PagedResources<TapDefinitionResource> taps = tapOperations().list();

		final Table table = new Table();
		table.addHeader(1, new TableHeader("Tap Name")).addHeader(2, new TableHeader("Stream Name"))
				.addHeader(3, new TableHeader("Tap Definition")).addHeader(4, new TableHeader("Status"));

		for (TapDefinitionResource tapDefinitionResource : taps.getContent()) {
			final TableRow row = new TableRow();
			row.addValue(1, tapDefinitionResource.getName()).addValue(2, tapDefinitionResource.getStreamName())
					.addValue(3, tapDefinitionResource.getDefinition());
			if (Boolean.TRUE.equals(tapDefinitionResource.isDeployed())) {
				row.addValue(4, "deployed");
			}
			else {
				row.addValue(4, "");
			}
			table.getRows().add(row);
		}

		return table;
	}

	@CliCommand(value = DESTROY_TAP, help = "Destroy existing tap(s)")
	public String destroyTap(
			@CliOption(key = { "", "name" }, help = "the name of the tap to destroy", optionContext = "existing-tap disable-string-converter") String name,
			@CliOption(key = { "all" }, help = "destroy all the existing taps", unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean all) {
		String message = "";
		switch (Assertions.exactlyOneOf("name", name, "all", all)) {
			case 0:
				tapOperations().destroy(name);
				message = String.format("Destroyed tap '%s'", name);
				break;
			case 1:
				tapOperations().destroyAll();
				message = String.format("Destroyed all the taps");
				break;
			default:
				throw new IllegalArgumentException("You must specify exactly one of 'name', 'all'");
		}
		return message;
	}

	@CliCommand(value = DEPLOY_TAP, help = "Deploy previously created tap(s)")
	public String deployTap(
			@CliOption(key = { "", "name" }, help = "the name of the tap to deploy", optionContext = "existing-tap disable-string-converter") String name,
			@CliOption(key = { "all" }, help = "deploy all the un-deployed taps", unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean all) {
		String message = "";
		switch (Assertions.exactlyOneOf("name", name, "all", all)) {
			case 0:
				tapOperations().deploy(name);
				message = String.format("Deployed tap '%s'", name);
				break;
			case 1:
				tapOperations().deployAll();
				message = String.format("Deployed all the taps");
				break;
			default:
				throw new IllegalArgumentException("You must specify exactly one of 'name', 'all'");
		}
		return message;
	}

	@CliCommand(value = UNDEPLOY_TAP, help = "Un-deploy previously deployed tap(s)")
	public String undeployTap(
			@CliOption(key = { "", "name" }, help = "the name of the tap to un-deploy", optionContext = "existing-tap disable-string-converter") String name,
			@CliOption(key = { "all" }, help = "undeploy all the deployed taps", unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean all) {
		String message = "";
		switch (Assertions.exactlyOneOf("name", name, "all", all)) {
			case 0:
				tapOperations().undeploy(name);
				message = String.format("Un-deployed tap '%s'", name);
				break;
			case 1:
				tapOperations().undeployAll();
				message = String.format("Un-deployed all the taps");
				break;
			default:
				throw new IllegalArgumentException("You must specify exactly one of 'name', 'all'");
		}
		return message;
	}

	private TapOperations tapOperations() {
		return xdShell.getSpringXDOperations().tapOperations();
	}

}
