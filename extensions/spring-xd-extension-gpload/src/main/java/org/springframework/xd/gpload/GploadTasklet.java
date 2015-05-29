/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.xd.gpload;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.step.tasklet.x.AbstractProcessBuilderTasklet;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.StringUtils;

/**
 * Tasklet used for running gpload tool.
 *
 * Note: This this class is not thread-safe.
 *
 * @since 1.2
 * @author Thomas Risberg
 * @author Gary Russell
 */
public class GploadTasklet extends AbstractProcessBuilderTasklet implements InitializingBean {

	private static final String GPLOAD_COMMAND = "gpload";

	private static final String CONTROL_FILE_NODE_GPLOAD = "GPLOAD";

	private static final String CONTROL_FILE_NODE_INPUT = "INPUT";

	private static final String CONTROL_FILE_NODE_SOURCE = "SOURCE";

	private static final String CONTROL_FILE_NODE_FILE = "FILE";

	private String gploadHome;

	private String controlFile;

	private String options;

	private String inputSourceFile;

	private String database;

	private String host;

	private Integer port;

	private String username;

	private String password;

	private String passwordFile;

	private String controlFileToUse;


	public String getGploadHome() {
		return gploadHome;
	}

	public void setGploadHome(String gploadHome) {
		this.gploadHome = gploadHome;
	}

	public String getControlFile() {
		return controlFile;
	}

	public void setControlFile(String controlFile) {
		this.controlFile = controlFile;
	}

	public String getOptions() {
		return options;
	}

	public void setOptions(String options) {
		this.options = options;
	}

	public String getInputSourceFile() {
		return inputSourceFile;
	}

	public void setInputSourceFile(String inputSourceFile) {
		this.inputSourceFile = inputSourceFile;
	}

	public String getDatabase() {
		return database;
	}

	public void setDatabase(String database) {
		this.database = database;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getPasswordFile() {
		return passwordFile;
	}

	public void setPasswordFile(String passwordFile) {
		this.passwordFile = passwordFile;
	}

	@Override
	protected boolean isStoppable() {
		return false;
	}

	@Override
	protected List<String> createCommand() throws Exception {
		List<String> command = new ArrayList<String>();
		command.add(getCommandString());
		if (StringUtils.hasText(host)) {
			command.add("-h");
			command.add(host);
		}
		if (port != null) {
			command.add("-p");
			command.add(port.toString());
		}
		if (StringUtils.hasText(database)) {
			command.add("-d");
			command.add(database);
		}
		if (StringUtils.hasText(username)) {
			command.add("-U");
			command.add(username);
		}
		if (StringUtils.hasText(controlFile)) {
			if (StringUtils.hasText(inputSourceFile)) {
				try {
					controlFileToUse = createControlFile(inputSourceFile, controlFile);
				}
				catch (IOException e) {
					throw new JobExecutionException("Error while creating control file", e);
				}
			}
			else {
				controlFileToUse = controlFile;
			}
			command.add("-f");
			command.add(controlFileToUse);
		}
		if (StringUtils.hasText(options)) {
			command.add(options);
		}
		return command;
	}

	@Override
	protected String getCommandDisplayString() {
		return getCommandString();
	}

	@Override
	protected String getCommandName() {
		return "gpload";
	}

	@Override
	protected String getCommandDescription() {
		return "gpload job";
	}

	@Override
	public ExitStatus afterStep(StepExecution stepExecution) {
		stepExecution.getExecutionContext().put(getCommandName() + ".controlFile", controlFileToUse);
		return super.afterStep(stepExecution);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (!StringUtils.hasText(gploadHome)) {
			throw new IllegalArgumentException(
					"Missing gploadHome property, it is mandatory for running gpload command");
		}
		if (StringUtils.hasText(password) && StringUtils.hasText(passwordFile)) {
			throw new IllegalArgumentException("You can't specify both 'password' and 'passwordFile' options");
		}
		if (StringUtils.hasText(password)) {
			addEnvironmentProvider(new GploadEnvironmentProvider(gploadHome, false, password));
		}
		else if (StringUtils.hasText(passwordFile)) {
			addEnvironmentProvider(new GploadEnvironmentProvider(gploadHome, true, passwordFile));
		}
		else {
			throw new IllegalArgumentException("You must specify either 'password' or 'passwordFile' option");
		}
	}

	private String getCommandString() {
		return gploadHome + "/bin/" + GPLOAD_COMMAND;
	}

	private String createControlFile(String inputSourceFile, String controlFile) throws IOException {
		InputStream in = new FileInputStream(new File(controlFile));
		Yaml yaml = new Yaml();
		Object data = yaml.load(in);
		in.close();
		replaceInputSourceFile(data, inputSourceFile);
		String output = yaml.dump(data);
		File out = File.createTempFile("gpload-", ".yml");
		String outFile = out.getAbsolutePath();
		FileWriter f = new FileWriter(out);
		f.write(output);
		f.close();
		return outFile;
	}

	@SuppressWarnings("unchecked")
	protected static void replaceInputSourceFile(Object data, String inputSourceFile) {
		if (data != null && data instanceof Map) {
			Object gpload = ((Map) data).get(CONTROL_FILE_NODE_GPLOAD);
			if (gpload != null && gpload instanceof Map) {
				Object input = ((Map) gpload).get(CONTROL_FILE_NODE_INPUT);
				if (input == null) {
					input = new ArrayList<Object>();
					((Map) gpload).put(CONTROL_FILE_NODE_INPUT, input);
				}
				if (input instanceof List) {
					boolean hasSource = false;
					for (Object o : (List) input) {
						if (o instanceof Map && ((Map) o).containsKey(CONTROL_FILE_NODE_SOURCE)) {
							hasSource = true;
						}
					}
					if (!hasSource) {
						Map<String, Object> tmp = new LinkedHashMap<>();
						tmp.put(CONTROL_FILE_NODE_SOURCE, new LinkedHashMap<String, Object>());
						((List) input).add(tmp);
					}
					for (Object o : (List) input) {
						if (o instanceof Map && ((Map) o).containsKey(CONTROL_FILE_NODE_SOURCE)) {
							Object source = ((Map) o).get(CONTROL_FILE_NODE_SOURCE);
							if (source != null && source instanceof Map) {
								((Map) source).put(CONTROL_FILE_NODE_FILE, Arrays.asList(inputSourceFile));
							}
						}
					}
				}
			}
		}
	}

}
