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

	@Autowired
	private XDShell xdShell;

	@CliAvailabilityIndicator({ "deploy stream", "undeploy stream" })
	public boolean available() {
		return xdShell.getSpringXDOperations() != null;
	}

	@CliCommand(value = "deploy stream", help = "Deploy a new stream definition")
	public String deployStream(
	//
			@CliOption(mandatory = true, key = { "", "definition" }, help = "a stream definition, using XD DSL (e.g. \"http --port=9000 | hdfs\")")
			String dsl,//
			@CliOption(mandatory = true, key = "name", help = "the name to give to the stream")
			String name) {
		xdShell.getSpringXDOperations().deployStream(name, dsl);
		return String.format("Deployed new stream '%s'", name);
	}

	@CliCommand(value = "undeploy stream", help = "Undeploy a stream from the running XD container(s)")
	public String undeployStream(//
			@CliOption(mandatory = true, key = { "", "name" }, help = "the name of the stream to delete")
			String name) {
		xdShell.getSpringXDOperations().undeployStream(name);
		return String.format("Undeployed stream '%s'", name);
	}

}
