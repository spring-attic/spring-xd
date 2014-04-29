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

package org.springframework.xd.dirt.container.initializer;

import java.io.IOException;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.groovy.GroovyBeanDefinitionReader;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.StringUtils;

/**
 * A {@link OrderedContextInitializer} base class for loading XML or Groovy Bean Definitions into the target Container
 * Context.
 * 
 * @author David Turanski
 */
public abstract class AbstractResourceBeanDefinitionProvider implements OrderedContextInitializer {

	protected Logger logger = LoggerFactory.getLogger(getClass());

	private Pattern prefix = Pattern.compile("^[a-z*]+:");

	@Override
	public void onApplicationEvent(ApplicationPreparedEvent event) {
		ConfigurableApplicationContext context = event.getApplicationContext();

		PathMatchingResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver(context);

		XmlBeanDefinitionReader xmlReader = new XmlBeanDefinitionReader(
				(BeanDefinitionRegistry) context.getBeanFactory());
		xmlReader.setEnvironment(context.getEnvironment());

		GroovyBeanDefinitionReader groovyReader = new GroovyBeanDefinitionReader(
				(BeanDefinitionRegistry) context.getBeanFactory());
		groovyReader.setEnvironment(context.getEnvironment());


		for (String locationPattern : getLocations()) {
			try {
				Resource[] resources = resourceResolver.getResources(locationPattern);
				logger.info("resolving resource location pattern {}", locationPattern);
				for (Resource resource : resources) {
					if (resource.getFilename() != null) {
						if (resource.getFilename().endsWith(".xml")) {
							logger.info("loading XD extensions from {}", resource.getFilename());
							xmlReader.loadBeanDefinitions(resource);
						}
						else if (resource.getFilename().endsWith(".groovy")) {
							groovyReader.loadBeanDefinitions(resource);
						}
					}
				}
			}
			catch (IOException e) {
				logger.warn("could not resolve resources for {}", locationPattern);
			}

		}
	}

	/**
	 * Subclasses implement this to return extensions locations (null is ok).
	 * 
	 * @return extensions locations as a comma-delimited String
	 */
	protected abstract String getExtensionsLocations();

	/**
	 * This will prepend "classpath*:" to each location if no Resource prefix provided.
	 * 
	 * @return an array of locations
	 */
	protected String[] getLocations() {
		String[] locations = StringUtils.commaDelimitedListToStringArray(getExtensionsLocations());
		for (int i = 0; i < locations.length; i++) {
			if (!prefix.matcher(locations[i]).find()) {
				locations[i] = "classpath*:" + locations[i];
			}
			if (!locations[i].endsWith("/")) {
				locations[i] = locations[i] + "/";
			}
			locations[i] = locations[i] + "**/*.*";
		}
		return locations;
	}

}
