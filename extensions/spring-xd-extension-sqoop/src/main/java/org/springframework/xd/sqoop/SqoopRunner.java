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
import org.apache.hadoop.security.SecurityUtil;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.sqoop.Sqoop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.hadoop.configuration.ConfigurationFactoryBean;
import org.springframework.data.hadoop.configuration.ConfigurationUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
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
	public static final String SECURITY_AUTH_METHOD = "security.authMethod";
	public static final String SECURITY_USER_KEYTAB = "security.userKeytab";
	public static final String SECURITY_USER_PRINCIPAL = "security.userPrincipal";
	public static final String SECURITY_NAMENODE_PRINCIPAL = "security.namenodePrincipal";
	public static final String SECURITY_RM_MANAGER_PRINCIPAL = "security.rmManagerPrincipal";
	public static final String SECURITY_MAPREDUCE_JOBHISTORY_PRINCIPAL = "security.jobHistoryPrincipal";

	public static void main(String[] args) {

		if (args == null || args.length < 1) {
			throw new IllegalArgumentException("Missing arguments and/or configuration options for Sqoop");
		}

		String jarPath = org.apache.sqoop.util.Jars.getJarPathForClass(JobConf.class);
		String hadoopMapredHome = jarPath.substring(0, jarPath.lastIndexOf(File.separator));

		String command = args[0];
		String[] sqoopArgumentSource = args[1].split(" ");

		List<String> sqoopArguments = new ArrayList<String>();
		boolean provideConnect = parseSqoopCommandArguments(command, sqoopArgumentSource, sqoopArguments);

		logger.info("Sqoop command: " + command);
		logger.info("Using args: " + sqoopArguments);
		logger.info("Mapreduce home: " + hadoopMapredHome);

		Map<String, String> configOptions = new HashMap<String, String>();
		parseConfigOptions(args, configOptions);

		Configuration configuration = createConfiguration(configOptions);

		List<String> finalArguments = new ArrayList<String>();
		createFinalArguments(hadoopMapredHome, command, sqoopArguments, provideConnect, configOptions, finalArguments);

		String xdModuleLibjars = System.getenv("XD_MODULE_LIBJARS");
		if (StringUtils.hasText(xdModuleLibjars)) {
			logger.info("Adding --libjars to Sqoop job submission");
			String[] extraJars = StringUtils.commaDelimitedListToStringArray(xdModuleLibjars);
			List<URL> classPathUrls = new ArrayList<URL>();
			try {
				classPathUrls.addAll(Arrays.asList(((URLClassLoader) SqoopRunner.class.getClassLoader()).getURLs()));
			} catch (Exception ignore) {
			}
			List<Resource> libJars = new ArrayList<>();
			for (String jarName : extraJars) {
				for (URL url : classPathUrls) {
					if (url.getPath().endsWith(jarName)) {
						logger.info("Adding jar: " + url);
						libJars.add(new UrlResource(url));
					}
				}
			}
			ConfigurationUtils.addLibs(configuration, libJars.toArray(new Resource[libJars.size()]));
		}

		if (sqoopArguments.contains("--as-avrodatafile")) {
			logger.info("Adding Avro libraries to Sqoop job submission");
			List<URL> classPathUrls = new ArrayList<URL>();
			try {
				classPathUrls.addAll(Arrays.asList(((URLClassLoader) SqoopRunner.class.getClassLoader()).getURLs()));
			} catch (Exception ignore) {
			}
			List<Resource> libJars = new ArrayList<>();
			for (URL url : classPathUrls) {
				if (url.getPath().contains("avro-mapred-1.7") || url.getPath().contains("avro-1.7")) {
					logger.info("Adding jar: " + url);
					libJars.add(new UrlResource(url));
				}
			}
			ConfigurationUtils.addLibs(configuration, libJars.toArray(new Resource[libJars.size()]));
		}

		final int ret = Sqoop.runTool(finalArguments.toArray(new String[finalArguments.size()]), configuration);

		if (ret != 0) {
			throw new RuntimeException("Sqoop failed - return code "
					+ Integer.toString(ret));
		}
	}

	protected static void createFinalArguments(String hadoopMapredHome, String command, List<String> sqoopArguments,
	                                           boolean provideConnect, Map<String, String> configOptions,
	                                           List<String> finalArguments) {
		finalArguments.add(command);
		if (provideConnect) {
			finalArguments.add("--connect=" + configOptions.get(JDBC_URL_KEY));
			if (configOptions.containsKey(JDBC_USERNAME_KEY) && configOptions.get(JDBC_USERNAME_KEY) != null) {
				finalArguments.add("--username=" + configOptions.get(JDBC_USERNAME_KEY));
			}
			if (configOptions.containsKey(JDBC_PASSWORD_KEY) && configOptions.get(JDBC_PASSWORD_KEY) != null) {
				finalArguments.add("--password=" + configOptions.get(JDBC_PASSWORD_KEY));
			}
		}
		if (command.toLowerCase().startsWith("import") || command.toLowerCase().startsWith("export") ||
				command.toLowerCase().startsWith("create")) {
			finalArguments.add("--hadoop-mapred-home=" + hadoopMapredHome);
		}
		finalArguments.addAll(sqoopArguments);
	}

	protected static Configuration createConfiguration(Map<String, String> configOptions) {

		Configuration configuration = new Configuration();
		setConfigurationProperty(configOptions, configuration, CommonConfigurationKeys.FS_DEFAULT_NAME_KEY);
		setConfigurationProperty(configOptions, configuration, YarnConfiguration.RM_HOSTNAME);
		setConfigurationProperty(configOptions, configuration, YarnConfiguration.RM_ADDRESS);
		setConfigurationProperty(configOptions, configuration, YarnConfiguration.RM_SCHEDULER_ADDRESS);
		setConfigurationProperty(configOptions, configuration, YarnConfiguration.YARN_APPLICATION_CLASSPATH);
		setConfigurationProperty(configOptions, configuration, "mapreduce.framework.name");
		if (StringUtils.hasText(configOptions.get("mapreduce.jobhistory.address"))) {
			setConfigurationProperty(configOptions, configuration, "mapreduce.jobhistory.address");
		}
		if (configOptions.containsKey(SECURITY_AUTH_METHOD) && "kerberos".equals(configOptions.get(SECURITY_AUTH_METHOD))) {
			configuration.setBoolean("hadoop.security.authorization", true);
			configuration.set("hadoop.security.authentication", configOptions.get(SECURITY_AUTH_METHOD));
			configuration.set("dfs.namenode.kerberos.principal", configOptions.get(SECURITY_NAMENODE_PRINCIPAL));
			configuration.set("yarn.resourcemanager.principal", configOptions.get(SECURITY_RM_MANAGER_PRINCIPAL));
			if (StringUtils.hasText(configOptions.get(SECURITY_MAPREDUCE_JOBHISTORY_PRINCIPAL))) {
				configuration.set("mapreduce.jobhistory.principal", configOptions.get(SECURITY_MAPREDUCE_JOBHISTORY_PRINCIPAL));
			}
			String userKeytab = configOptions.get(SECURITY_USER_KEYTAB);
			String userPrincipal = configOptions.get(SECURITY_USER_PRINCIPAL);
			UserGroupInformation.setConfiguration(configuration);
			if (StringUtils.hasText(userKeytab)) {
				configuration.set(ConfigurationFactoryBean.USERKEYTAB, userKeytab.trim());
			}
			if (StringUtils.hasText(userPrincipal)) {
				configuration.set(ConfigurationFactoryBean.USERPRINCIPAL, userPrincipal.trim());
			}
			if (StringUtils.hasText(userKeytab) && StringUtils.hasText(userPrincipal)) {
				try {
					SecurityUtil.login(configuration, ConfigurationFactoryBean.USERKEYTAB, ConfigurationFactoryBean.USERPRINCIPAL);
				} catch (Exception e) {
					logger.warn("Cannot login using keytab " + userKeytab + " and principal " + userPrincipal, e);
				}
			}
		}

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

	protected static boolean parseSqoopCommandArguments(String command, String[] sqoopArguments, List<String> runtimeArguments) {
		boolean connectProvided = false;
		boolean connectNeeded = false;
		if (command.toLowerCase().startsWith("import") || command.toLowerCase().startsWith("export") ||
				command.toLowerCase().startsWith("create") || command.toLowerCase().startsWith("codegen") ||
				command.toLowerCase().startsWith("list")) {
			connectNeeded = true;
		}
		for (int i = 0; i < sqoopArguments.length; i++) {
			if (sqoopArguments[i].startsWith("--connect") || sqoopArguments[i].startsWith("--options-file")) {
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
		return connectNeeded && !connectProvided;
	}

}
