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

package org.springframework.xd.dirt.container.initializer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.Ordered;
import org.springframework.util.StringUtils;


/**
 * An {@link OrderedContextInitializer} to scan for annotation configured beans in xd.extensions.basepackages and in
 * resource locations xd.extensions.locations. Each property value is a comma delimited string.
 * 
 * @author David Turanski
 */
public class PluginContextExtensionsInitializer extends AbstractXMLBeanDefinitionProvider {

	@Value("${xd.extensions.locations:}")
	private String extensionsLocations;

	@Value("${xd.extensions.basepackages:}")
	private String extensionsBasePackages;

	@Override
	public void onApplicationEvent(ApplicationPreparedEvent event) {
		if (StringUtils.hasText(extensionsBasePackages)) {
			AnnotationConfigApplicationContext context = (AnnotationConfigApplicationContext) event.getApplicationContext();
			ClassPathScanningCandidateComponentProvider componentProvider = new
					ClassPathScanningCandidateComponentProvider(
							true, context.getEnvironment());

			for (String basePackage : StringUtils.commaDelimitedListToStringArray(extensionsBasePackages)) {
				for (BeanDefinition bean : componentProvider.findCandidateComponents(basePackage)) {
					context.registerBeanDefinition(BeanDefinitionReaderUtils.generateBeanName(bean, context), bean);
				}
			}
		}

		if (StringUtils.hasText(extensionsLocations)) {
			super.onApplicationEvent(event);
		}
	}

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

	@Override
	protected String[] getLocations() {
		String[] locations = StringUtils.commaDelimitedListToStringArray(this.extensionsLocations);
		for (int i = 0; i < locations.length; i++) {
			locations[i] = locations[i] + "/**/*.xml";
		}

		return locations;
	}

}
