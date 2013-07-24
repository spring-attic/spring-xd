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

import java.net.URI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.CommandLine;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;
import org.springframework.xd.rest.client.SpringXDOperations;
import org.springframework.xd.rest.client.impl.SpringXDTemplate;

@Component
public class XDShell implements CommandMarker, InitializingBean {

	private static final Log logger = LogFactory.getLog(XDShell.class);
	
	private String target;

	private SpringXDOperations springXDOperations;

	@Autowired
	private CommandLine commandLine;
	
	private String host = "localhost";
	
	private String port = "8080";
	
	public XDShell() {		

	}
	
	public String getTarget() {
		return target;
	}

	@CliCommand(value = { "target" }, help = "Select the XD admin server to use")
	public String target(
			@CliOption(mandatory = false, key = { "", "uri" }, help = "the location of the XD Admin REST endpoint", unspecifiedDefaultValue = "http://localhost:8080/")
			String target) {
		
		try {
			springXDOperations = new SpringXDTemplate(URI.create(target));
			this.target = target;
			return String.format("Successfully targeted %s", target);
		}
		catch (Exception e) {
			this.target = "unknown";
			springXDOperations = null;
			logger.warn("Unable to contact XD Admin - " + e.getMessage());
			return String.format("Unable to contact XD Admin at %s", target);			
		}
	}

	public SpringXDOperations getSpringXDOperations() {
		return springXDOperations;
	}
	
	private String getDefaultUri() {
		if (commandLine.getArgs() != null) {
			String[] args = commandLine.getArgs();
			int i = 0;
			while (i < args.length) {
				String arg = args[i++];
				if (arg.equals("--host")) {
					this.host = args[i++];
				} else if (arg.equals("--port")) {
					this.port = args[i++];
				} else {
					i--;
					break;
				}
			}
		}
		return "http://" + this.host + ":" + this.port;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		target(getDefaultUri());
	}

}
