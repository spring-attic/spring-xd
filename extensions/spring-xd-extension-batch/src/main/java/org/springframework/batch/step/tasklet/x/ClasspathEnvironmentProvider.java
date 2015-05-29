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

package org.springframework.batch.step.tasklet.x;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.StringUtils;

/**
 * @since 1.2
 * @author Thomas Risberg
 * @author Gary Russell
 */
public class ClasspathEnvironmentProvider implements EnvironmentProvider {

	private static final String XD_CONFIG_HOME = "xd.config.home";

	ConfigurableEnvironment environment;

	Class<?> taskletClass;


	public ClasspathEnvironmentProvider(ConfigurableEnvironment environment, Class<?> taskletClass) {
		this.environment = environment;
		this.taskletClass = taskletClass;
	}

	@Override
	public void setEnvironment(Map<String, String> env) {
		String classPath = createClassPath();
		env.put("CLASSPATH", classPath);
	}

	protected String createClassPath() {
		URLClassLoader serverClassLoader;
		URLClassLoader taskletClassLoader;
		try {
			serverClassLoader = (URLClassLoader) Class.forName("org.springframework.xd.dirt.core.Job").getClassLoader();
			taskletClassLoader = (URLClassLoader) taskletClass.getClassLoader();
		}
		catch (Exception e) {
			throw new IllegalStateException("Unable to determine classpath from ClassLoader.", e);
		}
		if (serverClassLoader == null) {
			throw new IllegalStateException("Unable to access ClassLoader for " + taskletClass + ".");
		}
		if (taskletClassLoader == null) {
			throw new IllegalStateException("Unable to access Context ClassLoader.");
		}
		List<String> classPath = new ArrayList<String>();
		String configHome = environment.getProperty(XD_CONFIG_HOME);
		if (StringUtils.hasText(configHome)) {
			classPath.add(configHome);
		}
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
			if (classPathBuilder.length() > 0) {
				classPathBuilder.append(separator);
			}
			classPathBuilder.append(url);
		}
		return classPathBuilder.toString();
	}

}
