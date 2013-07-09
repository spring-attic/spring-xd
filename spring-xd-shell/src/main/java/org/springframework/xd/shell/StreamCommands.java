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

package org.springframework.xd.shell;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;

@Component
public class StreamCommands implements CommandMarker {

	private static final String DEPLOY_STREAM = "stream deploy";

	private static final String UNDEPLOY_STREAM = "stream undeploy";

	private static final String CREATE_STREAM = "stream create";

	private static final String DESTROY_STREAM = "stream destroy";

	@Autowired
	private XDShell xdShell;

	@CliAvailabilityIndicator({ DEPLOY_STREAM, UNDEPLOY_STREAM, CREATE_STREAM, DESTROY_STREAM })
	public boolean available() {
		return xdShell.getSpringXDOperations() != null;
	}

	@CliCommand(value = CREATE_STREAM, help = "Create a new stream definition")
	public String createStream(
			//
			@CliOption(mandatory = true, key = { "definition" }, help = "a stream definition, using XD DSL (e.g. \"http --port=9000 | hdfs\")")
			String dsl,//
			@CliOption(mandatory = true, key = { "", "name" }, help = "the name to give to the stream")
			String name, //
			@CliOption(key = "deploy", help = "whether to deploy the stream immediately", unspecifiedDefaultValue = "true")
			boolean deploy) {
		xdShell.getSpringXDOperations().createStream(name, dsl, deploy);
		return String.format("Created new stream '%s'", name);
	}

	@CliCommand(value = DESTROY_STREAM, help = "Destroy an existing stream")
	public String destroyStream(//
			@CliOption(mandatory = true, key = { "", "name" }, help = "the name of the stream to destroy")
			String name) {
		xdShell.getSpringXDOperations().destroyStream(name);
		return String.format("Destroyed stream '%s'", name);
	}

	@CliCommand(value = DEPLOY_STREAM, help = "Deploy a previously created stream")
	public String deployStream(
			@CliOption(mandatory = true, key = { "", "name" }, help = "the name of the stream to deploy")
			String name) {
		xdShell.getSpringXDOperations().deployStream(name);
		return String.format("Deployed stream '%s'", name);
	}

	@CliCommand(value = UNDEPLOY_STREAM, help = "Un-deploy a previously deployed stream")
	public String undeployStream(
			@CliOption(mandatory = true, key = { "", "name" }, help = "the name of the stream to un-deploy")
			String name) {
		xdShell.getSpringXDOperations().undeployStream(name);
		return String.format("Un-deployed stream '%s'", name);
	}

}
