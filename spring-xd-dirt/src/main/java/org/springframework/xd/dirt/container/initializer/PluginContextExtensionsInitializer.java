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

import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.Ordered;


/**
 * An {@link OrderedContextInitializer} to scan for annotation configured beans in xd.extensions.basepackage and in
 * resource locations xd.extensions.location
 * 
 * @author David Turanski
 */
public class PluginContextExtensionsInitializer extends AbstractXMLBeanDefinitionProvider {

	@Value("${xd.extensions.location}")
	private String extensionsLocation;

	@Value("${xd.extensions.basepackage}")
	private String extensionsBasePackage;

	@Override
	public void onApplicationEvent(ApplicationPreparedEvent event) {

		AnnotationConfigApplicationContext context = (AnnotationConfigApplicationContext) event.getApplicationContext();
		ClassPathScanningCandidateComponentProvider componentProvider = new
				ClassPathScanningCandidateComponentProvider(
						true, context.getEnvironment());

		Set<BeanDefinition> beans = componentProvider.findCandidateComponents(extensionsBasePackage);

		for (BeanDefinition bean : beans) {
			context.registerBeanDefinition(BeanDefinitionReaderUtils.generateBeanName(bean, context), bean);
		}

		super.onApplicationEvent(event);
	}

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

	@Override
	protected String[] getLocations() {
		return new String[] { this.extensionsLocation + "/*.xml" };
	}

}
