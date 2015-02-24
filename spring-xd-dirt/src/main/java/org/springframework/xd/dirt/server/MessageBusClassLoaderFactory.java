/*
 *
 *  * Copyright 2011-2014 the original author or authors.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.springframework.xd.dirt.server;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.Assert;

/**
 * An ApplicationListener which tracks which MessageBus implementation ought to be used so that it exposes a ClassLoader
 * that knows about the correct jar files for that bus.
 * <p>Make sure that this listener is triggered before the created ClassLoader is used.</p>
 *
 * @author Eric Bottard
 * @author David Turanski
 */
public class MessageBusClassLoaderFactory implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

	public static final String MESSAGE_BUS_JARS_LOCATION = "file:${XD_HOME}/lib/messagebus/${XD_TRANSPORT}/*.jar";

	private PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

	private static URL[] getUrls(String... patterns) {
		PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
		List<URL> jars = new ArrayList<URL>();
		for (String pattern : patterns) {
			try {
				Resource[] resources = resolver.getResources(pattern);
				for (Resource resource : resources) {
					URL url = resource.getURL();
					if (!jars.contains(url)) {
						jars.add(url);
					}
				}
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}

		}
		return jars.toArray(new URL[jars.size()]);
	}

	@Override
	public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {

		String transport = event.getEnvironment().resolvePlaceholders("${XD_TRANSPORT}");
		if (!"local".equals(transport)) {
			String jarsLocation = event.getEnvironment().resolvePlaceholders(MESSAGE_BUS_JARS_LOCATION);

			((DefaultResourceLoader) resolver.getResourceLoader()).setClassLoader(makeClassLoader(jarsLocation));
		}
	}

	private ClassLoader makeClassLoader(String jarsLocation) {
		URL[] messageBusJars = getUrls(jarsLocation);
		Assert.notEmpty(messageBusJars, "Unable to locate any message bus implementation jars at location " +
				jarsLocation);
		return new URLClassLoader(messageBusJars, MessageBusClassLoaderFactory.class.getClassLoader());
	}

	public PathMatchingResourcePatternResolver getResolver() {
		return resolver;
	}
}
