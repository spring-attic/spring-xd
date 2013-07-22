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

import static org.springframework.xd.module.ModuleType.TRIGGER;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.xd.dirt.module.ResourceDefinitionException;
import org.springframework.xd.module.BeanDefinitionAddingPostProcessor;
import org.springframework.xd.module.Module;
import org.springframework.xd.module.Plugin;

/**
 * {@link Plugin} to enable the registration of triggers.
 *
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
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
		if (!TRIGGER.equals(module.getType())) {
			return;
		}
		Assert.notNull(commonApplicationContext,
				"The 'commonApplicationContext' property must not be null.");

		final BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition();
		List<String> triggersAdded = new ArrayList<String>();
		try {
			if (module.getProperties().containsKey(TriggerType.cron.name())) {
				triggersAdded.add(TriggerType.cron.name());
				commonApplicationContext.getBeanFactory().registerSingleton(
					BEAN_NAME_PREFIX + group,
					new CronTrigger(module.getProperties().getProperty(
							TriggerType.cron.name())));
		}
		if (module.getProperties().containsKey(TriggerType.fixedDelay.name())) {
				triggersAdded.add(TriggerType.fixedDelay.name());
			commonApplicationContext.getBeanFactory().registerSingleton(
					BEAN_NAME_PREFIX + group,
					new PeriodicTrigger(Long.parseLong(module.getProperties()
							.getProperty(TriggerType.fixedDelay.name()))));
		}
		if (module.getProperties().containsKey(TriggerType.fixedRate.name())) {
				triggersAdded.add(TriggerType.fixedRate.name());
			PeriodicTrigger fixedRateTrigger = new PeriodicTrigger(Long.parseLong(module.getProperties()
					.getProperty(TriggerType.fixedRate.name())));
			fixedRateTrigger.setFixedRate(true);
			commonApplicationContext.getBeanFactory().registerSingleton(
					BEAN_NAME_PREFIX + group, fixedRateTrigger);
		}
			if (triggersAdded.size() == 0) {
				throw new ResourceDefinitionException(
						"No valid trigger property. Expected one of: "
								+ "cron, fixedDelay or fixedRate");
			}
		final BeanDefinitionAddingPostProcessor postProcessor = new BeanDefinitionAddingPostProcessor();
		postProcessor.addBeanDefinition(BEAN_NAME_PREFIX + group, builder.getBeanDefinition());

		this.commonApplicationContext.addBeanFactoryPostProcessor(postProcessor);

		configureProperties(module, group);
		} catch (IllegalStateException ise) {
			if (triggersAdded.size() > 0) {
				throw new ResourceDefinitionException(
						"Only one trigger property allowed, but received: "
								+ StringUtils
										.collectionToCommaDelimitedString(triggersAdded));
			}
			throw ise;
		}
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

	/**
	 * Trigger type enum
	 */
	private enum TriggerType {
		cron ,
		fixedDelay ,
		fixedRate 
		};

}
