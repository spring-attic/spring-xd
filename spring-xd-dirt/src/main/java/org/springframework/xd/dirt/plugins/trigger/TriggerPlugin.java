/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.xd.dirt.plugins.trigger;

import java.util.Properties;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.util.Assert;
import org.springframework.xd.dirt.plugins.BeanDefinitionAddingPostProcessor;
import org.springframework.xd.module.Module;
import org.springframework.xd.module.Plugin;

/**
 * {@link Plugin} to enable the registration of triggers.
 *
 * @author Gunnar Hillert
 * @since 1.0
 *
 */
public class TriggerPlugin implements Plugin {

	public static final String BEAN_NAME_PREFIX = "trigger.";

	private ConfigurableApplicationContext commonApplicationContext;

	/**
	 * Processes a new {@link Trigger} being added. Currently, it supports adding
	 * {@link CronTrigger}s. The {@link Trigger} is added to the common
	 * {@link ConfigurableApplicationContext}.
	 */
	@Override
	public void processModule(Module module, String group, int index) {
		if (!"trigger".equalsIgnoreCase(module.getType())) {
			return;
		}

		final BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(CronTrigger.class);
		builder.addConstructorArgValue(module.getProperties().get("cron"));

		final BeanDefinitionAddingPostProcessor postProcessor = new BeanDefinitionAddingPostProcessor();
		postProcessor.addBeanDefinition(BEAN_NAME_PREFIX + group, builder.getBeanDefinition());

		Assert.notNull(commonApplicationContext, "The 'commonApplicationContext' property must not be null.");
		this.commonApplicationContext.addBeanFactoryPostProcessor(postProcessor);

		configureProperties(module, group);
		this.commonApplicationContext.refresh();

	}

	@Override
	public void removeModule(Module module, String group, int index) {
	}

	private void configureProperties(Module module, String group) {
		Properties properties = new Properties();
		properties.setProperty("xd.stream.name", group);
		module.addProperties(properties);
	}

	/**
	 * Will add the {@link ConfigurableApplicationContext} as an instance variable,
	 * so it becomes available when registering a new Module.
	 */
	@Override
	public void postProcessSharedContext(ConfigurableApplicationContext context) {
		this.commonApplicationContext = context;
	}

}
