/*
 * Copyright 2014 the original author or authors.
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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;

import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.shell.CommandLine;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.xd.rest.client.impl.SpringXDTemplate;
import org.springframework.xd.shell.Configuration;
import org.springframework.xd.shell.Target;
import org.springframework.xd.shell.Target.TargetStatus;
import org.springframework.xd.shell.XDShell;
import org.springframework.xd.shell.util.CommonUtils;
import org.springframework.xd.shell.util.UiUtils;

/**
 * Defines several shell commands in order to configure the shell itself. The
 * commands will set properties on the {@link Configuration} object.
 *
 * @author Gunnar Hillert
 * @author Marius Bogoevici
 * @since 1.0
 *
 */
@Component
public class ConfigCommands implements CommandMarker, InitializingBean {

	private static final Logger logger = LoggerFactory.getLogger(XDShell.class);

	@Autowired
	private CommandLine commandLine;

	@Autowired
	private Configuration configuration;

	@Autowired
	private XDShell xdShell;

	@Autowired
	private UserInput userInput;

	public ConfigCommands() {
	}

	@CliCommand(value = {"admin config server"}, help = "Configure the XD admin server to use")
	public String target(
			@CliOption(mandatory = false, key = {"", "uri"},
					help = "the location of the XD Admin REST endpoint",
					unspecifiedDefaultValue = Target.DEFAULT_TARGET) String targetUriString,
			@CliOption(mandatory = false, key = {"username"},
					help = "the username for authenticated access to the Admin REST endpoint",
					unspecifiedDefaultValue = Target.DEFAULT_USERNAME) String targetUsername,
			@CliOption(mandatory = false, key = {"password"},
					help = "the password for authenticated access to the Admin REST endpoint (valid only with a username)",
					specifiedDefaultValue = Target.DEFAULT_SPECIFIED_PASSWORD,
					unspecifiedDefaultValue = Target.DEFAULT_UNSPECIFIED_PASSWORD) String targetPassword) {

		try {
			if (!StringUtils.isEmpty(targetPassword) && StringUtils.isEmpty(targetUsername)) {
				return "A password may be specified only together with a username";
			}
			if (Target.DEFAULT_SPECIFIED_PASSWORD.equalsIgnoreCase(targetPassword) && !StringUtils.isEmpty(targetUsername)) {
				// read password from the command line
				targetPassword = userInput.prompt("Password", "", false);
			}
			configuration.setTarget(new Target(targetUriString, targetUsername, targetPassword));
			if (configuration.getTarget().getTargetCredentials() != null) {
				BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
				credentialsProvider.setCredentials(AuthScope.ANY,
						new UsernamePasswordCredentials(
								configuration.getTarget().getTargetCredentials().getUsername(),
								configuration.getTarget().getTargetCredentials().getPassword()));
				CloseableHttpClient httpClient = HttpClientBuilder.create().setDefaultCredentialsProvider(credentialsProvider).build();
				HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
				this.xdShell.setSpringXDOperations(new SpringXDTemplate(requestFactory, configuration.getTarget().getTargetUri()));
			}
			else {
				this.xdShell.setSpringXDOperations(new SpringXDTemplate(configuration.getTarget().getTargetUri()));
			}
			configuration.getTarget().setTargetResultMessage(
					String.format("Successfully targeted %s", configuration.getTarget().getTargetUri()));
		}
		catch (Exception e) {
			configuration.getTarget().setTargetException(e);
			this.xdShell.setSpringXDOperations(null);
			configuration.getTarget().setTargetResultMessage(
					String.format("Unable to contact XD Admin Server at '%s'.",
							targetUriString));
			if (logger.isTraceEnabled()) {
				logger.trace(configuration.getTarget().getTargetResultMessage(), e);
			}
		}

		return configuration.getTarget().getTargetResultMessage();
	}

	@CliCommand(value = {"admin config info"}, help = "Show the XD admin server being used")
	public String info() {

		final Map<String, String> statusValues = new TreeMap<String, String>();

		final Target target = configuration.getTarget();

		statusValues.put("Target", target.getTargetUriAsString());
		if (target.getTargetCredentials() != null) {
			statusValues.put("Credentials", target.getTargetCredentials().getDisplayableContents());
		}
		statusValues.put("Result", target.getTargetResultMessage() != null ? target.getTargetResultMessage() : "");
		statusValues.put("Timezone used", CommonUtils.getTimeZoneNameWithOffset(this.configuration.getClientTimeZone()));

		final StringBuilder sb = new StringBuilder(UiUtils.renderParameterInfoDataAsTable(statusValues, false, 66));

		if (TargetStatus.ERROR.equals(target.getStatus())) {
			sb.append(UiUtils.HORIZONTAL_LINE);
			sb.append("An exception ocurred during targeting:\n");

			final StringWriter stringWriter = new StringWriter();
			target.getTargetException().printStackTrace(new PrintWriter(stringWriter));

			sb.append(stringWriter.toString());
		}
		return sb.toString();
	}

	/**
	 * Retrieve a list of available {@link TimeZone} Ids via a Spring XD Shell command.
	 */
	@CliCommand(value = "admin config timezone list", help = "List all timezones")
	public String listTimeZones() {
		final StringBuilder timeZones = new StringBuilder();

		for (String timeZone : TimeZone.getAvailableIDs()) {
			timeZones.append(timeZone + "\n");
		}

		return timeZones.toString();
	}

	/**
	 * Allows for setting the {@link TimeZone} via a Spring XD Shell command.
	 */
	@CliCommand(value = "admin config timezone set", help = "Set the timezone of the Spring XD Shell (Not persisted)")
	public String setTimeZone(
			@CliOption(mandatory = true, key = {"", "timeZone"}, help = "the id of the timezone, "
					+ "You can obtain a list of timezone ids using 'admin config timezone list', "
					+ "If an invalid timezone id is provided, then 'Greenwich Mean Time' "
					+ "is being used") String timeZoneId) {
		final TimeZone newCientTimeZone = TimeZone.getTimeZone(timeZoneId);
		this.configuration.setClientTimeZone(newCientTimeZone);
		return "TimeZone set to " + newCientTimeZone.getDisplayName();
	}

	/**
	 * Initialize the default {@link Target} for the XD Admin Server. It will use
	 * the constants {@link Target#DEFAULT_HOST}, {@link Target#DEFAULT_PORT}
	 * and {@link Target#DEFAULT_SCHEME}.
	 *
	 * Alternatively, the host and port can also be set using the {@code --host}
	 * and {@code --port} command line parameters.
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		target(getDefaultUri().toString(), getDefaultUsername(), getDefaultPassword());
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
		return new URI(Target.DEFAULT_SCHEME, null, host, port, null, null, null);
	}

	private String getDefaultUsername() {
		int indexOfUserParameter = ArrayUtils.indexOf(commandLine.getArgs(), "--username");
		// if '--username' exists and it is not the last in the list of arguments, the next argument is the password
		if (indexOfUserParameter >= 0 && indexOfUserParameter < commandLine.getArgs().length - 1) {
			return commandLine.getArgs()[indexOfUserParameter + 1];
		}
		else {
			return Target.DEFAULT_USERNAME;
		}
	}

	private String getDefaultPassword() {
		int indexOfPasswordParameter = ArrayUtils.indexOf(commandLine.getArgs(), "--password");
		// if '--password' exists and it is not the last in the list of arguments, the next argument is the password
		if (indexOfPasswordParameter >= 0) {
			if (indexOfPasswordParameter < commandLine.getArgs().length - 1) {
				return commandLine.getArgs()[indexOfPasswordParameter + 1];
			}
			else {
				return Target.DEFAULT_SPECIFIED_PASSWORD;
			}
		}
		else {
			return Target.DEFAULT_SPECIFIED_PASSWORD;
		}
	}

}
