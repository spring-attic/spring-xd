/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.yarn.shell;

import java.util.ArrayList;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.ApplicationReport;

import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.shell.event.ParseResult;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.xd.shell.util.Table;
import org.springframework.yarn.app.bootclient.YarnBootClientInfoApplication;
import org.springframework.yarn.client.YarnClient;

/**
 * Shell integration providing control commands for Spring XD on Hadoop Yarn.
 * <p>
 * XD on Yarn command set follows logic of:
 * <ul>
 * <li>Submit new XD application instance</li>
 * <li>Query existing instances</li>
 * <li>Kill running instances</li>
 * </ul>
 * 
 * @author Janne Valkealahti
 * 
 */
@Component
public class YarnXdCommands extends YarnCommandsSupport {

	private static final String PREFIX = "yarn xd ";

	private static final String COMMAND_INSTALL = PREFIX + "install";

	private static final String COMMAND_LIST_SUBMITTED = PREFIX + "list submitted";

	private static final String COMMAND_SUBMIT = PREFIX + "submit";

	private static final String COMMAND_KILL = PREFIX + "kill";

	private static final String OPTION_INSTALL_ID = "id";

	private static final String OPTION_LIST_SUBMITTED_VERBOSE = "verbose";

	private static final String HELP_COMMAND_INSTALL = "Install application bundle into hdfs";

	private static final String HELP_COMMAND_LIST_SUBMITTED = "List XD application instances on Yarn";

	private static final String HELP_OPTION_LIST_SUBMITTED_VERBOSE = "verbose output";

	private static final String HELP_SUBMIT = "Submit new XD instance to Yarn";

	private static final String HELP_SUBMIT_COUNT = "container count - as how many xd instances should be created";

	private static final String HELP_SUBMIT_REDISHOST = "Redis host - if used, default localhost";

	private static final String HELP_SUBMIT_REDISPORT = "Redis port - if used, default 6379";

	private static final String HELP_KILL = "Kill running XD instance on Yarn";

	private static final String HELP_KILL_ID = "application id - as shown by Yarn resource manager or shell list command";

	@Override
	protected boolean configurationChanged() throws Exception {
		return true;
	}

	@Override
	protected boolean propertiesChanged() throws Exception {
		return true;
	}

	// @CliAvailabilityIndicator({ COMMAND_LIST, COMMAND_SUBMIT, COMMAND_KILL })
	// public boolean available() {
	// // we have yarn if YarnConfiguration class can be found
	// return ClassUtils.isPresent("org.apache.hadoop.yarn.conf.YarnConfiguration", getClass().getClassLoader());
	// }

	@Override
	public ParseResult beforeInvocation(ParseResult invocationContext) {
		invocationContext = super.beforeInvocation(invocationContext);
		String defaultNameKey = (String) ReflectionUtils.getField(
				ReflectionUtils.findField(FileSystem.class, "FS_DEFAULT_NAME_KEY"), null);
		String fs = getConfiguration().get(defaultNameKey);
		String hdrm = getConfiguration().get("yarn.resourcemanager.address");

		if (StringUtils.hasText(fs) && StringUtils.hasText(hdrm)) {
			return invocationContext;
		}
		else {
			log.error("You must set fs URL and rm address before running yarn commands");
			throw new RuntimeException("You must set fs URL and rm address before running yarn commands");
		}
	}

	@CliCommand(value = COMMAND_INSTALL, help = HELP_COMMAND_INSTALL)
	public String install(
			@CliOption(key = OPTION_INSTALL_ID, mandatory = true) String id,
			@CliOption(key = "count", help = HELP_SUBMIT_COUNT, unspecifiedDefaultValue = "1") String count,
			@CliOption(key = "redisHost", help = HELP_SUBMIT_REDISHOST, specifiedDefaultValue = "localhost") String redisHost,
			@CliOption(key = "redisPort", help = HELP_SUBMIT_REDISPORT, specifiedDefaultValue = "6379") String redisPort) {
		Assert.state(StringUtils.hasText(id), "Id must be set");

		ArrayList<String> args = new ArrayList<String>();

		int c = Integer.parseInt(count);
		if (c < 1 || c > 10) {
			throw new IllegalArgumentException("Illegal container count [" + c + "]");
		}
		args.add("--spring.yarn.appmaster.containerCount=" + count);

		if (StringUtils.hasText(redisHost)) {
			args.add("--spring.redis.host=" + redisHost);
		}
		if (StringUtils.hasText(redisPort)) {
			// bail out with error if not valid port
			int port = Integer.parseInt(redisPort);
			if (port < 1 || port > 65535) {
				throw new RuntimeException("Port " + port + " not valid");
			}
			args.add("--spring.redis.port=" + redisPort);
		}


		installApplication(new String[] { "yarn" }, id, args.toArray(new String[0]));
		return "Successfully installed new application instance";
	}

	/**
	 * Submits new Spring XD instance to Hadoop Yarn.
	 * 
	 * @return the Command message
	 * @throws Exception if error occurred
	 */
	@CliCommand(value = COMMAND_SUBMIT, help = HELP_SUBMIT)
	public String submit(
			@CliOption(key = "id", mandatory = true) String id)
			throws Exception {
		ApplicationId applicationId = submitApplication(new String[] { "yarn" }, id);
		if (applicationId != null) {
			return "Submitted new XD instance with containers, application id is "
					+ applicationId.toString();
		}
		throw new IllegalArgumentException("Failed to submit new application instance.");

	}

	/**
	 * Command listing application instances known to Yarn resource manager.
	 * 
	 * @param id the application to be killed
	 * @return the Command message
	 * @throws Exception if error occurred
	 */
	@CliCommand(value = COMMAND_KILL, help = HELP_KILL)
	public String kill(@CliOption(key = { "", "id" }, mandatory = true, help = HELP_KILL_ID) final String id)
			throws Exception {
		ApplicationId applicationId = null;
		YarnClient yarnClient = getYarnClient();
		for (ApplicationReport a : yarnClient.listApplications()) {
			if (a.getApplicationId().toString().equals(id)) {
				applicationId = a.getApplicationId();
				break;
			}
		}
		if (applicationId != null) {
			yarnClient.killApplication(applicationId);
		}
		else {
			throw new IllegalArgumentException("Application id " + id + " not found.");
		}

		return "Killed app " + id;
	}

	/**
	 * Command listing known application instances known to Yarn resource manager.
	 * 
	 * @return The {@link Table} of known application instances.
	 * @throws Exception if error occurred
	 */
	@CliCommand(value = COMMAND_LIST_SUBMITTED, help = HELP_COMMAND_LIST_SUBMITTED)
	public String list(
			@CliOption(key = OPTION_LIST_SUBMITTED_VERBOSE, help = HELP_OPTION_LIST_SUBMITTED_VERBOSE, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean verbose)
			throws Exception {

		ArrayList<String> args = new ArrayList<String>();
		args.add("--spring.yarn.YarnBootClientInfoApplication.operation=SUBMITTED");
		args.add("--spring.yarn.YarnBootClientInfoApplication.verbose=" + verbose);
		args.add("--spring.yarn.YarnBootClientInfoApplication.type=XD");
		args.add("--spring.yarn.YarnBootClientInfoApplication.headers.ORIGTRACKURL=XD ADMIN URL");
		return new YarnBootClientInfoApplication().info("", null,
				getConfigurationProperties().getMergedProperties("foo"),
				getConfiguration(), args.toArray(new String[0]));
	}

}
