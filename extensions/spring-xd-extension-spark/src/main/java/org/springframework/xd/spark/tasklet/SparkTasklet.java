/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.xd.spark.tasklet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link Tasklet} for running Spark application.
 *
 * @author Thomas Risberg
 * @author Ilayaperumal Gopinathan
 */
public class SparkTasklet implements Tasklet, EnvironmentAware, StepExecutionListener {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private static final String MODULE_HOME = "xd.module.home";

	private static final String LIB_PATTERN = "/job/sparkapp/lib/*.jar";

	private static final String SPARK_SUBMIT_CLASS = "org.apache.spark.deploy.SparkSubmit";

	/**
	 * Exit code of Spark app
	 */
	private int exitCode = -1;

	/**
	 * Spark application name
	 */
	private String name;

	/**
	 * Spark master URL
	 */
	private String master;

	/**
	 * Spark application's main class
	 */
	private String mainClass;

	/**
	 * Path to a bundled jar that includes your application and its
	 * dependencies excluding spark
	 */
	private String appJar;

	/**
	 * Comma separated list of config key-value pairs to Spark application
	 */
	private String conf;

	/**
	 * Comma separated list of files to be placed in the
	 * working directory of each executor
	 */
	private String files;

	/**
	 * Program arguments for the application main class.
	 */
	private String programArgs;

	private ConfigurableEnvironment environment;

	private ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

	public String getMaster() {
		return master;
	}

	public void setMaster(String master) {
		this.master = master;
	}

	public String getMainClass() {
		return mainClass;
	}

	public void setMainClass(String mainClass) {
		this.mainClass = mainClass;
	}

	public String getAppJar() {
		return appJar;
	}

	public void setAppJar(String appJar) {
		this.appJar = appJar;
	}

	public String getProgramArgs() {
		return programArgs;
	}

	public void setProgramArgs(String programArgs) {
		this.programArgs = programArgs;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getConf() {
		return conf;
	}

	public void setConf(String conf) {
		this.conf = conf;
	}

	public String getFiles() {
		return files;
	}

	public void setFiles(String files) {
		this.files = files;
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = (ConfigurableEnvironment) environment;
	}

	@Override
	public RepeatStatus execute(StepContribution contribution,
			ChunkContext chunkContext) throws Exception {

		StepExecution stepExecution = chunkContext.getStepContext().getStepExecution();
		ExitStatus exitStatus = stepExecution.getExitStatus();

		String moduleHome = environment.getProperty(MODULE_HOME);
		Assert.notNull(moduleHome, "Module home must not be null.");
		Resource[] resources = resolver.getResources(moduleHome + LIB_PATTERN);
		ArrayList<String> dependencies = new ArrayList<String>();
		for (int i = 0; i < resources.length; i++) {
			dependencies.add(resources[i].getURL().getFile());
		}
		ArrayList<String> args = new ArrayList<String>();
		if (StringUtils.hasText(name)) {
			args.add("--name");
			args.add(name);
		}
		args.add("--class");
		args.add(mainClass);
		args.add("--master");
		args.add(master);
		args.add("--deploy-mode");
		args.add("client");
		if (StringUtils.hasText(conf)) {
			Collection<String> configs = StringUtils.commaDelimitedListToSet(conf);
			for (String config : configs) {
				args.add("--conf");
				args.add(config.trim());
			}
		}
		if (StringUtils.hasText(files)) {
			args.add("--files");
			args.add(files);
		}
		args.add("--jars");
		args.add(StringUtils.collectionToCommaDelimitedString(dependencies));
		if (StringUtils.hasText(appJar)) {
			args.add(appJar);
		}
		if (StringUtils.hasText(programArgs)) {
			args.addAll(StringUtils.commaDelimitedListToSet(programArgs));
		}
		List<String> sparkCommand = new ArrayList<String>();
		sparkCommand.add("java");
		sparkCommand.add(SPARK_SUBMIT_CLASS);
		sparkCommand.addAll(args);

		URLClassLoader serverClassLoader;
		URLClassLoader taskletClassLoader;
		try {
			serverClassLoader = (URLClassLoader) Class.forName("org.springframework.xd.dirt.core.Job").getClassLoader();
			taskletClassLoader = (URLClassLoader) this.getClass().getClassLoader();
		}
		catch (Exception e) {
			throw new IllegalStateException("Unable to determine classpath from ClassLoader.", e);
		}

		List<String> classPath = new ArrayList<String>();
		for (URL url : serverClassLoader.getURLs()) {
			String file = url.getFile().split("\\!/", 2)[0];
			if (file.endsWith(".jar")) {
				classPath.add(file);
			}
		}
		for (URL url : taskletClassLoader.getURLs()) {
			String file = url.getFile().split("\\!/", 2)[0];
			if (file.endsWith(".jar") && !classPath.contains(file)) {
				classPath.add(file);
			}
		}
		StringBuilder classPathBuilder = new StringBuilder();
		String separator = System.getProperty("path.separator");
		for (String url : classPath) {
			if (!url.contains("logback")) {
				if (classPathBuilder.length() > 0) {
					classPathBuilder.append(separator);
				}
				classPathBuilder.append(url);
			}
		}

		ProcessBuilder pb = new ProcessBuilder(sparkCommand).redirectErrorStream(true);
		Map<String, String> env = pb.environment();
		env.put("CLASSPATH", classPathBuilder.toString());
		String msg = "Spark application '" + mainClass + "' is being launched";
		StringBuilder sparkCommandString = new StringBuilder();
		for (String cmd : sparkCommand) {
			sparkCommandString.append(cmd).append(" ");
		}
		stepExecution.getExecutionContext().putString("spark.command", sparkCommandString.toString());
		List<String> sparkLog = new ArrayList<String>();
		try {
			Process p = pb.start();
			p.waitFor();
			exitCode = p.exitValue();
			msg = "Spark application '" + mainClass + "' finished with exit code: " + exitCode;
			if (exitCode == 0) {
				logger.info(msg);
			}
			else {
				logger.error(msg);
			}
			sparkLog = getProcessOutput(p);
			p.destroy();
		}
		catch (IOException e) {
			msg = "Starting Spark application '" + mainClass + "' failed with: " + e;
			logger.error(msg);
		}
		catch (InterruptedException e) {
			msg = "Executing Spark application '" + mainClass + "' failed with: " + e;
			logger.error(msg);
		}
		finally {
			printLog(sparkLog, exitCode);
			StringBuilder firstException = new StringBuilder();
			if (exitCode != 0) {
				for (String line : sparkLog) {
					if (firstException.length() == 0) {
						if (line.contains("Exception")) {
							firstException.append(line).append("\n");
						}
					}
					else {
						if (line.startsWith("\t")) {
							firstException.append(line).append("\n");
						}
						else {
							break;
						}
					}
				}
				if (firstException.length() > 0) {
					msg = msg + "\n" + firstException.toString();
				}
			}
			StringBuilder sparkLogMessages = new StringBuilder();
			for (String line : sparkLog) {
				sparkLogMessages.append(line).append("</br>");
			}
			stepExecution.getExecutionContext().putString("spark.log", sparkLogMessages.toString());
			stepExecution.setExitStatus(exitStatus.addExitDescription(msg));
		}

		return RepeatStatus.FINISHED;
	}

	@Override
	public ExitStatus afterStep(StepExecution stepExecution) {
		if (exitCode == 0) {
			return ExitStatus.COMPLETED;
		}
		else {
			return ExitStatus.FAILED;
		}
	}

	@Override
	public void beforeStep(StepExecution stepExecution) {
	}

	private List<String> getProcessOutput(Process p) {
		List<String> lines = new ArrayList<String>();
		if (p == null) {
			return lines;
		}
		InputStream in = p.getInputStream();
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		String line;
		try {
			while ((line = reader.readLine()) != null) {
				lines.add(line);
			}
		}
		catch (IOException ignore) {
		}
		finally {
			try {
				reader.close();
			}
			catch (IOException e) {
			}
		}
		return lines;
	}

	private void printLog(List<String> lines, int exitCode) {
		if (exitCode != 0) {
			for (String line : lines) {
				logger.error("Spark Logger: " + line);
			}
		}
		else {
			if (logger.isDebugEnabled()) {
				for (String line : lines) {
					logger.debug("Spark Logger: " + line);
				}
			}
		}
	}
}
