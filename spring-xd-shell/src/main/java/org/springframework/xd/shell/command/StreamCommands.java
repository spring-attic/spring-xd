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
import org.springframework.xd.rest.client.StreamOperations;
import org.springframework.xd.rest.client.domain.StreamDefinitionResource;
import org.springframework.xd.shell.XDShell;
import org.springframework.xd.shell.util.Assertions;
import org.springframework.xd.shell.util.Table;
import org.springframework.xd.shell.util.TableHeader;
import org.springframework.xd.shell.util.TableRow;

@Component
public class StreamCommands implements CommandMarker {

	private static final String CREATE_STREAM = "stream create";

	private static final String LIST_STREAM = "stream list";

	private static final String DEPLOY_STREAM = "stream deploy";

	private static final String UNDEPLOY_STREAM = "stream undeploy";

	private static final String DESTROY_STREAM = "stream destroy";

	@Autowired
	private XDShell xdShell;

	@CliAvailabilityIndicator({ CREATE_STREAM, LIST_STREAM, DEPLOY_STREAM, UNDEPLOY_STREAM, DESTROY_STREAM })
	public boolean available() {
		return xdShell.getSpringXDOperations() != null;
	}

	@CliCommand(value = CREATE_STREAM, help = "Create a new stream definition")
	public String createStream(
			@CliOption(mandatory = true, key = { "", "name" }, help = "the name to give to the stream") String name,
			@CliOption(mandatory = true, key = { "definition" }, help = "a stream definition, using XD DSL (e.g. \"http --port=9000 | hdfs\")") String dsl,
			@CliOption(key = "deploy", help = "whether to deploy the stream immediately", unspecifiedDefaultValue = "true") boolean deploy) {
		streamOperations().createStream(name, dsl, deploy);
		return String.format("Created new stream '%s'", name);
	}

	@CliCommand(value = DESTROY_STREAM, help = "Destroy existing stream(s)")
	public String destroyStream(
			//
			@CliOption(key = { "", "name" }, help = "the name of the stream to destroy", optionContext = "existing-stream disable-string-converter") String name,
			@CliOption(key = { "all" }, help = "destroy all the existing streams", unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean all) {
		String message = "";
		switch (Assertions.exactlyOneOf("name", name, "all", all)) {
			case 0:
				streamOperations().destroy(name);
				message = String.format("Destroyed stream '%s'", name);
				break;
			case 1:
				streamOperations().destroyAll();
				message = String.format("Destroyed all the streams");
				break;
			default:
				throw new IllegalArgumentException("You must specify exactly one of 'name', 'all'");
		}
		return message;
	}

	@CliCommand(value = DEPLOY_STREAM, help = "Deploy previously created stream(s)")
	public String deployStream(
			@CliOption(key = { "", "name" }, help = "the name of the stream to deploy", optionContext = "existing-stream undeployed disable-string-converter") String name,
			@CliOption(key = { "all" }, help = "deploy all un-deployed streams", unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean all) {
		String message = "";
		switch (Assertions.exactlyOneOf("name", name, "all", all)) {
			case 0:
				streamOperations().deploy(name);
				message = String.format("Deployed stream '%s'", name);
				break;
			case 1:
				streamOperations().deployAll();
				message = String.format("Deployed all the streams");
				break;
			default:
				throw new IllegalArgumentException("You must specify exactly one of 'name', 'all'");
		}
		return message;
	}

	@CliCommand(value = UNDEPLOY_STREAM, help = "Un-deploy previously deployed stream(s)")
	public String undeployStream(
			@CliOption(key = { "", "name" }, help = "the name of the stream to un-deploy", optionContext = "existing-stream deployed disable-string-converter") String name,
			@CliOption(key = { "all" }, help = "undeploy all the deployed streams", unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean all) {
		String message = "";
		switch (Assertions.exactlyOneOf("name", name, "all", all)) {
			case 0:
				streamOperations().undeploy(name);
				message = String.format("Un-deployed stream '%s'", name);
				break;
			case 1:
				streamOperations().undeployAll();
				message = String.format("Un-deployed all the streams");
				break;
			default:
				throw new IllegalArgumentException("You must specify exactly one of 'name', 'all'");
		}
		return message;
	}

	@CliCommand(value = LIST_STREAM, help = "List created streams")
	public Table listStreams() {

		final PagedResources<StreamDefinitionResource> streams = streamOperations().list();

		final Table table = new Table();
		table.addHeader(1, new TableHeader("Stream Name")).addHeader(2, new TableHeader("Stream Definition")).addHeader(3, new TableHeader("Status"));

		for (StreamDefinitionResource stream : streams) {
			final TableRow row = table.newRow();
			row.addValue(1, stream.getName()).addValue(2, stream.getDefinition());
			if (Boolean.TRUE.equals(stream.isDeployed())) {
				row.addValue(3, "deployed");
			} else {
				row.addValue(3, "");
			}
		}

		return table;

	}

	private StreamOperations streamOperations() {
		return xdShell.getSpringXDOperations().streamOperations();
	}
}
