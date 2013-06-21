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

import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;

@Component
public class XdShell implements CommandMarker {

	private String target = "http://localhost:8080";

	public String getTarget() {
		return target;
	}

	@CliCommand(value = { "target" }, help = "Select the XD admin server to use")
	public String target(@CliOption(mandatory = true, key = "") String target) {
		this.target = target;
		return String.format("Successfully targeted %s", target);
	}

	@CliCommand(value = "deploy stream", help = "Deploy a new stream definition")
	public String deployStream(@CliOption(mandatory = true, key = { "",
			"definition" }) String dsl, @CliOption(key = "name") String name) {
		return String.format("Deployed new stream '%s'", name);
	}

	@CliCommand(value = "undeploy stream", help = "Undeploy a stream from the running XD container(s)")
	public String undeployStream(@CliOption(mandatory = true, key = { "",
			"name" }) String name) {
		return String.format("Undeployed stream '%s'", name);
	}
}
