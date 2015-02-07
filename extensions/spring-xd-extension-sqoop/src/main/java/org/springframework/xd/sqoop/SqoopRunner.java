/*
 * Copyright 2014-2015 the original author or authors.
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

import org.springframework.util.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
	private static final String SPRING_HADOOP_CONFIG_PREFIX = "spring.hadoop.config";

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

		logger.info("Sqoop command: " + command);
		logger.info("Using args: " + sqoopArguments);
		logger.info("Mapreduce home: " + hadoopMapredHome);

		Map<String, String> configOptions = new HashMap<String, String>();
		parseConfigOptions(args, configOptions);

		Configuration configuration = createConfiguration(configOptions);

		List<String> finalArguments = new ArrayList<String>();
		createFinalArguments(hadoopMapredHome, command, sqoopArguments, connectProvided, configOptions, finalArguments);

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
		setConfigurationProperty(configOptions, configuration, CommonConfigurationKeys.FS_DEFAULT_NAME_KEY);
		setConfigurationProperty(configOptions, configuration, YarnConfiguration.RM_ADDRESS);
		setConfigurationProperty(configOptions, configuration, YarnConfiguration.YARN_APPLICATION_CLASSPATH);
		setConfigurationProperty(configOptions, configuration, "mapreduce.framework.name");
		for (Entry<String, String> entry : configOptions.entrySet()) {
			String key = entry.getKey();
			if (key.startsWith(SPRING_HADOOP_CONFIG_PREFIX + ".")) {
				String prop = key.substring(SPRING_HADOOP_CONFIG_PREFIX.length() + 1);
				String value = entry.getValue();
				logger.info("Setting configuration property: " + prop + "=" + value);
				configuration.set(prop, value);
			}
		}
		return configuration;
	}

	private static void setConfigurationProperty(Map<String, String> configOptions, Configuration configuration, String key) {
		if (configOptions.containsKey(key) && StringUtils.hasText(configOptions.get(key))) {
			String value = configOptions.get(key);
			logger.info("Setting configuration property: " + key + "=" + value);
			configuration.set(key, value);
		}
	}

	protected static void parseConfigOptions(String[] args, Map<String, String> configOptions) {
		for (int i = 2; i < args.length; i++) {
			String option = args[i];
			if (!option.contains("=")) {
				throw new IllegalArgumentException("Invalid config option provided: " + option);
			}
			String[] optionParts = option.split("=");
			StringBuilder value = new StringBuilder();
			if (optionParts.length > 1) {
				for (int j = 1; j < optionParts.length; j++) {
					if (j > 1) {
						value.append("=");
					}
					value.append(optionParts[j]);
				}
			}
			configOptions.put(optionParts[0], value.length() > 1 ? value.toString() : null);
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
