/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.xd.sqoop;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CommonConfigurationKeys;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.sqoop.Sqoop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class for running Sqoop tool.
 *
 * @since 1.1
 * @author Thomas Risberg
 */
public class SqoopRunner {

	private static final Logger logger = LoggerFactory.getLogger(SqoopRunner.class);

	private static final String JDBC_USERNAME_KEY = "jdbc.username";
	private static final String JDBC_PASSWORD_KEY = "jdbc.password";
	private static final String JDBC_URL_KEY = "jdbc.url";

	public static void main(String[] args) {

		if (args == null || args.length < 1) {
			throw new IllegalArgumentException("Missing arguments and/or configuration options for Sqoop");
		}

		String jarPath = org.apache.sqoop.util.Jars.getJarPathForClass(JobConf.class);
		String hadoopMapredHome = jarPath.substring(0, jarPath.lastIndexOf(File.separator));

		String command = args[0];
		String[] sqoopArgumentSource = args[1].split(" ");

		List<String> sqoopArguments = new ArrayList<String>();
		boolean connectProvided = parseSqoopArguments(sqoopArgumentSource, sqoopArguments);

		if (logger.isDebugEnabled()) {
			logger.debug("Sqoop command: " + command);
			logger.debug("Using args: " + sqoopArguments);
			logger.debug("Mapreduce home: " + hadoopMapredHome);
		}

		Map<String, String> configOptions = new HashMap<String, String>();
		parseConfigOptions(args, configOptions);

		Configuration configuration = createConfiguration(configOptions);

		List<String> finalArguments = new ArrayList<String>();
		createFinalArguments(hadoopMapredHome, command, sqoopArguments, connectProvided, configOptions, finalArguments);

		if (logger.isDebugEnabled()) {
			logger.debug("Final args: " + finalArguments);
		}

		final int ret = Sqoop.runTool(finalArguments.toArray(new String[finalArguments.size()]), configuration);

		if (ret != 0) {
			throw new RuntimeException("Sqoop failed - return code "
					+ Integer.toString(ret));
		}
	}

	protected static void createFinalArguments(String hadoopMapredHome, String command, List<String> sqoopArguments,
	                                           boolean connectProvided, Map<String, String> configOptions,
	                                           List<String> finalArguments) {
		finalArguments.add(command);
		if (!connectProvided) {
			finalArguments.add("--connect=" + configOptions.get(JDBC_URL_KEY));
			if (configOptions.containsKey(JDBC_USERNAME_KEY) && configOptions.get(JDBC_USERNAME_KEY) != null) {
				finalArguments.add("--username=" + configOptions.get(JDBC_USERNAME_KEY));
			}
			if (configOptions.containsKey(JDBC_PASSWORD_KEY) && configOptions.get(JDBC_PASSWORD_KEY) != null) {
				finalArguments.add("--password=" + configOptions.get(JDBC_PASSWORD_KEY));
			}
		}
		if ("import".equals(command.toLowerCase()) || "export".equals(command.toLowerCase())) {
			finalArguments.add("--hadoop-mapred-home=" + hadoopMapredHome);
		}
		finalArguments.addAll(sqoopArguments);
	}

	protected static Configuration createConfiguration(Map<String, String> configOptions) {
		Configuration configuration = new Configuration();
		if (configOptions.containsKey(CommonConfigurationKeys.FS_DEFAULT_NAME_KEY) &&
				configOptions.get(CommonConfigurationKeys.FS_DEFAULT_NAME_KEY) != null) {
			configuration.set(CommonConfigurationKeys.FS_DEFAULT_NAME_KEY,
					configOptions.get(CommonConfigurationKeys.FS_DEFAULT_NAME_KEY));
		}
		if (configOptions.containsKey(YarnConfiguration.RM_ADDRESS) &&
				configOptions.get(YarnConfiguration.RM_ADDRESS) != null) {
			configuration.set(YarnConfiguration.RM_ADDRESS,
					configOptions.get(YarnConfiguration.RM_ADDRESS));
		}
		configuration.set("mapreduce.framework.name", "yarn");
		return configuration;
	}

	protected static void parseConfigOptions(String[] args, Map<String, String> configOptions) {
		for (int i = 2; i < args.length; i++) {
			String option = args[i];
			if (!option.contains("=")) {
				throw new IllegalArgumentException("Invalid config option provided: " + option);
			}
			String[] optionParts = option.split("=");
			configOptions.put(optionParts[0], optionParts.length > 1 ? optionParts[1] : null);
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Config options: " + configOptions);
		}
	}

	protected static boolean parseSqoopArguments(String[] sqoopArguments, List<String> runtimeArguments) {
		boolean connectProvided = false;
		for (int i = 0; i < sqoopArguments.length; i++) {
			if (sqoopArguments[i].startsWith("--connect")) {
				connectProvided = true;
			}
			if (sqoopArguments[i].contains("\"")) {
				StringBuilder quotedArg = new StringBuilder();
				while(i < sqoopArguments.length) {
					if (quotedArg.length() > 0) {
						quotedArg.append(" ");
					}
					quotedArg.append(sqoopArguments[i]);
					if (sqoopArguments[i].endsWith("\"")) {
						break;
					}
					i++;
				}
				runtimeArguments.add(quotedArg.toString());
			}
			else {
				runtimeArguments.add(sqoopArguments[i]);
			}
		}
		return connectProvided;
	}

}
