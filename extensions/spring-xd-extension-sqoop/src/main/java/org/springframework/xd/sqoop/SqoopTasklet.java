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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.springframework.batch.step.tasklet.x.AbstractProcessBuilderTasklet;
import org.springframework.batch.step.tasklet.x.ClasspathEnvironmentProvider;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.util.StringUtils;

/**
 * Tasklet used for running Sqoop tool.
 *
 * Note: This this class is not thread-safe.
 *
 * @since 1.1
 * @author Thomas Risberg
 * @author Gary Russell
 */
public class SqoopTasklet extends AbstractProcessBuilderTasklet implements InitializingBean {

	private static final String SQOOP_RUNNER_CLASS = "org.springframework.xd.sqoop.SqoopRunner";

	private static final String SPRING_HADOOP_CONFIG_PREFIX = "spring.hadoop.config";

	private static final String SPRING_HADOOP_PREFIX = "spring.hadoop";

	private String[] arguments;

	private Properties hadoopProperties;

	private String libjars;


	public String[] getArguments() {
		return arguments;
	}

	public void setArguments(String[] arguments) {
		this.arguments = Arrays.copyOf(arguments, arguments.length);
	}

	public Properties getHadoopProperties() {
		return hadoopProperties;
	}

	public void setHadoopProperties(Properties hadoopProperties) {
		this.hadoopProperties = hadoopProperties;
	}

	public void setLibjars(String libjars) {
		this.libjars = libjars;
	}

	@Override
	protected boolean isStoppable() {
		return false;
	}

	@Override
	protected List<String> createCommand() {
		List<String> command = new ArrayList<String>();
		String javaHome = System.getenv("JAVA_HOME");
		if (StringUtils.hasText(javaHome)) {
			command.add(javaHome + "/bin/java");
		}
		else {
			command.add("java");
		}
		command.add(SQOOP_RUNNER_CLASS);
		command.addAll(Arrays.asList(arguments));
		Iterator<PropertySource<?>> i = environment.getPropertySources().iterator();
		while (i.hasNext()) {
			PropertySource<?> p = i.next();
			if (p instanceof EnumerablePropertySource) {
				for (String name : ((EnumerablePropertySource) p).getPropertyNames()) {
					if (name.startsWith(SPRING_HADOOP_CONFIG_PREFIX)) {
						command.add(name + "=" + environment.getProperty(name));
					}
				}
			}
		}
		for (Map.Entry<?, ?> e : hadoopProperties.entrySet()) {
			if (e.getKey().toString().startsWith(SPRING_HADOOP_PREFIX)) {
				command.add(e.getKey() + "=" + e.getValue());
			}
			else {
				command.add(SPRING_HADOOP_CONFIG_PREFIX + "." + e.getKey() + "=" + e.getValue());
			}
		}
		return command;
	}

	@Override
	protected String getCommandDisplayString() {
		if (arguments.length > 1) {
			return arguments[0] + " " + arguments[1];
		}
		else {
			return arguments[0];
		}
	}

	@Override
	protected String getCommandName() {
		return "Sqoop";
	}

	@Override
	protected String getCommandDescription() {
		return "Sqoop job for '" + arguments[0] + "'";
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (arguments == null || arguments.length < 1) {
			throw new IllegalArgumentException("Missing arguments and/or configuration options for Sqoop");
		}
		addEnvironmentProvider(new SqoopEnvironmentProvider(environment, libjars));
		addEnvironmentProvider(new ClasspathEnvironmentProvider(environment, this.getClass()));
	}
}
