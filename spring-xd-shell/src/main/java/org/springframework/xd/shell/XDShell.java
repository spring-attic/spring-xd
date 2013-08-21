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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.CommandLine;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.JLineLogHandler;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;
import org.springframework.xd.rest.client.SpringXDOperations;
import org.springframework.xd.rest.client.impl.SpringXDTemplate;
import org.springframework.xd.shell.Target.TargetStatus;
import org.springframework.xd.shell.util.UiUtils;

/**
 * @author Mark Pollack
 * @author Gunnar Hillert
 * @since 1.0
 * 
 */
@Component
public class XDShell implements CommandMarker, InitializingBean {

	private static final Log logger = LogFactory.getLog(XDShell.class);

	@Autowired
	private CommandLine commandLine;

	private SpringXDOperations springXDOperations;

	private Target target;

	public XDShell() {
	}

	/**
	 * Return the {@link Target} which encapsulates not only the Target URI but also success/error messages + status.
	 * 
	 * @return Should not never be null.
	 */
	public Target getTarget() {
		return target;
	}

	@CliCommand(value = { "admin config server" }, help = "Configure the XD admin server to use")
	public String target(@CliOption(mandatory = false, key = { "", "uri" },
			help = "the location of the XD Admin REST endpoint",
			unspecifiedDefaultValue = Target.DEFAULT_TARGET) String targetUriString) {

		try {
			this.target = new Target(targetUriString);
			this.springXDOperations = new SpringXDTemplate(this.target.getTargetUri());
			this.target.setTargetResultMessage(String.format("Successfully targeted %s", target.getTargetUri()));
		}
		catch (Exception e) {
			this.target.setTargetException(e);
			this.springXDOperations = null;
			this.target.setTargetResultMessage(String.format("Unable to contact XD Admin Server at '%s'.",
					targetUriString));

			if (logger.isTraceEnabled()) {
				logger.trace(this.target.getTargetResultMessage(), e);
			}
		}

		return this.target.getTargetResultMessage();
	}

	@CliCommand(value = { "admin config info" }, help = "Show the XD admin server being used")
	public String info() {

		final Map<String, String> statusValues = new TreeMap<String, String>();

		statusValues.put("Target", this.target.getTargetUriAsString());
		statusValues.put("Result", this.target.getTargetResultMessage());

		final StringBuilder sb = new StringBuilder(UiUtils.renderParameterInfoDataAsTable(statusValues, false, 66));

		if (TargetStatus.ERROR.equals(this.target.getStatus())) {
			sb.append(UiUtils.HORIZONTAL_LINE);
			sb.append("An exception ocurred during targeting:\n");

			final StringWriter stringWriter = new StringWriter();
			this.target.getTargetException().printStackTrace(new PrintWriter(stringWriter));

			sb.append(stringWriter.toString());
		}
		return sb.toString();
	}

	public SpringXDOperations getSpringXDOperations() {
		return springXDOperations;
	}

	private URI getDefaultUri() throws URISyntaxException {

		int port = Target.DEFAULT_PORT;
		String host = Target.DEFAULT_HOST;

		if (commandLine.getArgs() != null) {
			String[] args = commandLine.getArgs();
			int i = 0;
			while (i < args.length) {
				String arg = args[i++];
				if (arg.equals("--host")) {
					host = args[i++];
				}
				else if (arg.equals("--port")) {
					port = Integer.valueOf(args[i++]);
				}
				else {
					i--;
					break;
				}
			}
		}
		return new URI("http", null, host, port, null, null, null);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		target(getDefaultUri().toString());
	}

	// Set suppress duplicate messages to false
	// to allow XD shell to display all the messages on the console
	static {
		JLineLogHandler.setSuppressDuplicateMessages(false);
	}

}
