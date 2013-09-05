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

package org.springframework.xd.shell.hadoop;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Properties;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.ApplicationReport;
import org.apache.hadoop.yarn.api.records.LocalResourceType;
import org.apache.hadoop.yarn.api.records.YarnApplicationState;

import org.springframework.shell.core.ExecutionProcessor;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.shell.event.ParseResult;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.xd.shell.util.Table;
import org.springframework.xd.shell.util.TableHeader;
import org.springframework.xd.shell.util.TableRow;
import org.springframework.xd.yarn.XdYarnAppmasterRunner;
import org.springframework.yarn.YarnSystemConstants;
import org.springframework.yarn.client.YarnClient;
import org.springframework.yarn.client.YarnClientFactoryBean;
import org.springframework.yarn.configuration.EnvironmentFactoryBean;
import org.springframework.yarn.fs.LocalResourcesFactoryBean;
import org.springframework.yarn.fs.LocalResourcesFactoryBean.CopyEntry;
import org.springframework.yarn.fs.LocalResourcesFactoryBean.TransferEntry;
import org.springframework.yarn.launch.LaunchCommandsFactoryBean;

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
public class YarnCommands extends ConfigurationAware implements ExecutionProcessor {

	private static final String PREFIX = "yarn ";

	private static final String COMMAND_LIST = PREFIX + "list";

	private static final String COMMAND_SUBMIT = PREFIX + "submit";

	private static final String COMMAND_KILL = PREFIX + "kill";

	private static final String HELP_LIST = "List XD instances on Yarn";

	private static final String HELP_SUBMIT = "Submit new XD instance to Yarn";

	private static final String HELP_KILL = "Kill running XD instance on Yarn";

	private static final String HELP_KILL_ID = "application id - as shown by Yarn resource manager or shell list command";

	@Override
	protected boolean configurationChanged() throws Exception {
		return true;
	}

	@CliAvailabilityIndicator({ COMMAND_LIST, COMMAND_SUBMIT, COMMAND_KILL })
	public boolean available() {
		// we have yarn if YarnConfiguration class can be found
		return ClassUtils.isPresent("org.apache.hadoop.yarn.conf.YarnConfiguration", getClass().getClassLoader());
	}

	@Override
	public ParseResult beforeInvocation(ParseResult invocationContext) {
		invocationContext = super.beforeInvocation(invocationContext);
		String defaultNameKey = (String) ReflectionUtils.getField(
				ReflectionUtils.findField(FileSystem.class, "FS_DEFAULT_NAME_KEY"), null);
		String fs = getHadoopConfiguration().get(defaultNameKey);
		String hdrm = getHadoopConfiguration().get("yarn.resourcemanager.address");

		if (StringUtils.hasText(fs) && StringUtils.hasText(hdrm)) {
			return invocationContext;
		}
		else {
			LOG.severe("You must set fs URL and rm address before running yarn commands");
			throw new RuntimeException("You must set fs URL and rm address before running yarn commands");
		}
	}

	/**
	 * Submits new Spring XD instance to Hadoop Yarn.
	 * 
	 * @return the Command message
	 * @throws Exception if error occurred
	 */
	@CliCommand(value = COMMAND_SUBMIT, help = HELP_SUBMIT)
	public String submit() throws Exception {
		ApplicationId applicationId = getYarnClientForSubmit().submitApplication();
		if (applicationId != null) {
			return "Submit succesful, new application id is " + applicationId.toString();
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
	@CliCommand(value = COMMAND_LIST, help = HELP_LIST)
	public Table list() throws Exception {
		Table table = new Table();
		table.addHeader(1, new TableHeader("Id"))
				.addHeader(2, new TableHeader("User"))
				.addHeader(3, new TableHeader("Name"))
				.addHeader(4, new TableHeader("Queue"))
				.addHeader(5, new TableHeader("StartTime"))
				.addHeader(6, new TableHeader("FinishTime"))
				.addHeader(7, new TableHeader("State"))
				.addHeader(8, new TableHeader("FinalStatus"))
				.addHeader(9, new TableHeader("XD Admin"));

		for (ApplicationReport a : getYarnClient().listApplications()) {

			String xdAdminUrl = "";
			if (a.getYarnApplicationState() == YarnApplicationState.RUNNING) {
				xdAdminUrl = "http://" + a.getTrackingUrl().split("//")[1];
			}

			final TableRow row = new TableRow();
			row.addValue(1, a.getApplicationId().toString())
					.addValue(2, a.getUser())
					.addValue(3, a.getName())
					.addValue(4, a.getQueue())
					.addValue(5, DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(
							new Date(a.getStartTime())))
					.addValue(6, DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(
							new Date(a.getFinishTime())))
					.addValue(7, a.getYarnApplicationState().toString())
					.addValue(8, a.getFinalApplicationStatus().toString())
					.addValue(9, xdAdminUrl);
			table.getRows().add(row);
		}
		return table;
	}

	/**
	 * Builds a new {@link YarnClient}.
	 * 
	 * @return the {@link YarnClient}
	 * @throws Exception if error occurred
	 */
	private YarnClient getYarnClient() throws Exception {
		YarnClientFactoryBean factory = new YarnClientFactoryBean();
		factory.setConfiguration(getHadoopConfiguration());
		factory.afterPropertiesSet();
		return factory.getObject();
	}

	/**
	 * Builds a new {@link YarnClient}. This is needed for every submit operation because currently every client
	 * instance will only support one application instance.
	 * 
	 * @return the {@link YarnClient}
	 * @throws Exception if error occurred
	 */
	private YarnClient getYarnClientForSubmit() throws Exception {
		YarnClientFactoryBean yarnClientFactory = new YarnClientFactoryBean();

		Configuration configuration = getHadoopConfiguration();
		yarnClientFactory.setConfiguration(configuration);

		// localresources
		LocalResourcesFactoryBean localResourcesFactory = new LocalResourcesFactoryBean();
		localResourcesFactory.setConfiguration(configuration);
		Collection<TransferEntry> transferEntries = new ArrayList<TransferEntry>();
		transferEntries.add(new TransferEntry(LocalResourceType.ARCHIVE, null, "/xd/xdapp.zip", null, null, false));
		localResourcesFactory.setHdfsEntries(transferEntries);
		localResourcesFactory.setCopyEntries(new ArrayList<CopyEntry>());
		localResourcesFactory.afterPropertiesSet();
		yarnClientFactory.setResourceLocalizer(localResourcesFactory.getObject());

		// environment
		EnvironmentFactoryBean environmentFactory = new EnvironmentFactoryBean();
		environmentFactory.setClasspath("./xdapp.zip/lib/*");
		environmentFactory.setDefaultYarnAppClasspath(true);
		environmentFactory.setDelimiter(":");
		environmentFactory.setIncludeBaseDirectory(true);
		environmentFactory.afterPropertiesSet();
		yarnClientFactory.setEnvironment(environmentFactory.getObject());

		// commands
		LaunchCommandsFactoryBean launchCommandsFactory = new LaunchCommandsFactoryBean();
		launchCommandsFactory.setRunner(XdYarnAppmasterRunner.class);
		launchCommandsFactory.setContextFile("appmaster-context.xml");
		launchCommandsFactory.setBeanName(YarnSystemConstants.DEFAULT_ID_APPMASTER);
		Properties arguments = new Properties();
		arguments.put("container-count", "2");
		arguments.put("-DXD_STORE", "redis");
		arguments.put("-DXD_TRANSPORT", "redis");
		arguments.put("-DXD_HOME", "./xdapp.zip");
		launchCommandsFactory.setArguments(arguments);
		launchCommandsFactory.setStdout("<LOG_DIR>/Appmaster.stdout");
		launchCommandsFactory.setStderr("<LOG_DIR>/Appmaster.stderr");
		launchCommandsFactory.afterPropertiesSet();
		yarnClientFactory.setCommands(launchCommandsFactory.getObject());

		// build the client
		yarnClientFactory.setAppName("xdapp");
		yarnClientFactory.afterPropertiesSet();
		return yarnClientFactory.getObject();
	}

}
