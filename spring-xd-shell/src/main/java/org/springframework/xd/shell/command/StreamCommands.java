/*
 * Copyright 2013-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.shell.command;

import static org.springframework.xd.shell.command.DeploymentOptionKeys.PROPERTIES_FILE_OPTION;
import static org.springframework.xd.shell.command.DeploymentOptionKeys.PROPERTIES_OPTION;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.PagedResources;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;
import org.springframework.xd.rest.client.StreamOperations;
import org.springframework.xd.rest.domain.StreamDefinitionResource;
import org.springframework.xd.rest.domain.support.DeploymentPropertiesFormat;
import org.springframework.xd.shell.XDShell;
import org.springframework.xd.shell.util.Assertions;
import org.springframework.xd.shell.util.Table;
import org.springframework.xd.shell.util.TableHeader;

@Component
public class StreamCommands implements CommandMarker {

	private static final String CREATE_STREAM = "stream create";

	private static final String LIST_STREAM = "stream list";

	private static final String DEPLOY_STREAM = "stream deploy";

	private static final String UNDEPLOY_STREAM = "stream undeploy";

	private static final String UNDEPLOY_STREAM_ALL = "stream all undeploy";

	private static final String DESTROY_STREAM = "stream destroy";

	private static final String DESTROY_STREAM_ALL = "stream all destroy";

	@Autowired
	private UserInput userInput;

	@Autowired
	private XDShell xdShell;

	@CliAvailabilityIndicator({ CREATE_STREAM, LIST_STREAM, DEPLOY_STREAM, UNDEPLOY_STREAM, DESTROY_STREAM,
		DESTROY_STREAM_ALL, UNDEPLOY_STREAM_ALL })
	public boolean available() {
		return xdShell.getSpringXDOperations() != null;
	}

	@CliCommand(value = CREATE_STREAM, help = "Create a new stream definition")
	public String createStream(
			@CliOption(mandatory = true, key = { "", "name" }, help = "the name to give to the stream") String name,
			@CliOption(mandatory = true, key = { "definition" }, optionContext = "completion-stream disable-string-converter", help = "a stream definition, using XD DSL (e.g. \"http --port=9000 | hdfs\")") String dsl,
			@CliOption(key = "deploy", help = "whether to deploy the stream immediately", unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean deploy) {
		streamOperations().createStream(name, dsl, deploy);
		return (deploy) ? String.format("Created and deployed new stream '%s'", name) : String.format(
				"Created new stream '%s'", name);
	}

	@CliCommand(value = DESTROY_STREAM, help = "Destroy an existing stream")
	public String destroyStream(
			@CliOption(key = { "", "name" }, help = "the name of the stream to destroy", mandatory = true, optionContext = "existing-stream disable-string-converter") String name) {
		streamOperations().destroy(name);
		return String.format("Destroyed stream '%s'", name);
	}

	@CliCommand(value = DESTROY_STREAM_ALL, help = "Destroy all existing streams")
	public String destroyAllStreams(
			@CliOption(key = "force", help = "bypass confirmation prompt", unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean force) {
		// Be sure to short-circuit prompt if force is true
		if (force || "y".equalsIgnoreCase(userInput.promptWithOptions("Really destroy all streams?", "n", "y", "n"))) {
			streamOperations().destroyAll();
			return "Destroyed all streams";
		}
		else {
			return "";
		}
	}

	@CliCommand(value = DEPLOY_STREAM, help = "Deploy a previously created stream")
	public String deployStream(
			@CliOption(key = { "", "name" }, help = "the name of the stream to deploy", mandatory = true, optionContext = "existing-stream undeployed disable-string-converter") String name,
			@CliOption(key = { PROPERTIES_OPTION }, help = "the properties for this deployment", mandatory = false) String properties,
			@CliOption(key = { PROPERTIES_FILE_OPTION }, help = "the properties for this deployment (as a File)", mandatory = false) File propertiesFile
			) throws IOException {

		int which = Assertions.atMostOneOf(PROPERTIES_OPTION, properties, PROPERTIES_FILE_OPTION, propertiesFile);
		Map<String, String> propertiesToUse;
		switch (which) {
			case 0:
				propertiesToUse = DeploymentPropertiesFormat.parseDeploymentProperties(properties);
				break;
			case 1:
				Properties props = new Properties();
				try (FileInputStream fis = new FileInputStream(propertiesFile)) {
					props.load(fis);
				}
				propertiesToUse = DeploymentPropertiesFormat.convert(props);
				break;
			case -1: // Neither option specified
				propertiesToUse = Collections.<String, String> emptyMap();
				break;
			default:
				throw new AssertionError();
		}

		streamOperations().deploy(name, propertiesToUse);
		return String.format("Deployed stream '%s'", name);
	}

	@CliCommand(value = UNDEPLOY_STREAM, help = "Un-deploy a previously deployed stream")
	public String undeployStream(
			@CliOption(key = { "", "name" }, help = "the name of the stream to un-deploy", mandatory = true, optionContext = "existing-stream deployed disable-string-converter") String name
			) {
		streamOperations().undeploy(name);
		return String.format("Un-deployed stream '%s'", name);
	}

	@CliCommand(value = UNDEPLOY_STREAM_ALL, help = "Un-deploy all previously deployed stream")
	public String undeployAllStreams(
			@CliOption(key = "force", help = "bypass confirmation prompt", unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean force
			) {
		if (force || "y".equalsIgnoreCase(userInput.promptWithOptions("Really undeploy all streams?", "n", "y", "n"))) {
			streamOperations().undeployAll();
			return String.format("Un-deployed all the streams");
		}
		else {
			return "";
		}
	}

	@CliCommand(value = LIST_STREAM, help = "List created streams")
	public Table listStreams() {

		final PagedResources<StreamDefinitionResource> streams = streamOperations().list();

		final Table table = new Table()
				.addHeader(1, new TableHeader("Stream Name"))
				.addHeader(2, new TableHeader("Stream Definition"))
				.addHeader(3, new TableHeader("Status"));

		for (StreamDefinitionResource stream : streams) {
			table.newRow()
					.addValue(1, stream.getName())
					.addValue(2, stream.getDefinition())
					.addValue(3, stream.getStatus());
		}
		return table;
	}

	/*default*/ StreamOperations streamOperations() {
		return xdShell.getSpringXDOperations().streamOperations();
	}
}
