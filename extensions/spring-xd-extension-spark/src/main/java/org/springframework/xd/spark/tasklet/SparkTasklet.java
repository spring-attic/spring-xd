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

import java.util.ArrayList;

import org.apache.spark.deploy.SparkSubmit;

import org.springframework.batch.core.StepContribution;
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
 * {@link Tasklet} for running Spark job.
 *
 * @author Thomas Risberg
 * @author Ilayaperumal Gopinathan
 */
public class SparkTasklet implements Tasklet, EnvironmentAware {

	private static final String MODULE_HOME = "xd.module.home";

	private static final String LIB_PATTERN = "/job/sparkjob/lib/*.jar";

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

	@Override
	public RepeatStatus execute(StepContribution contribution,
			ChunkContext chunkContext) throws Exception {
		String moduleHome = environment.getProperty(MODULE_HOME);
		Assert.notNull(moduleHome, "Module home must not be null.");
		Resource[] resources = resolver.getResources(moduleHome + LIB_PATTERN);
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < resources.length; i++) {
			if (i == (resources.length - 1)) {
				builder.append(resources[i].getURL().getFile());
			}
			else {
				builder.append(resources[i].getURL().getFile() + ",");
			}
		}
		ArrayList<String> args = new ArrayList<String>();
		args.add("--class");
		args.add(mainClass);
		args.add("--master");
		args.add(master);
		args.add("--deploy-mode");
		args.add("client");
		args.add("--jars");
		args.add(builder.toString());
		args.add(appJar);
		if (StringUtils.hasText(programArgs)) {
			args.add(programArgs);
		}
		SparkSubmit.main(args.toArray(new String[args.size()]));
		System.out.println("Spark job ran successfully.");
		return RepeatStatus.FINISHED;
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = (ConfigurableEnvironment) environment;
	}
}
