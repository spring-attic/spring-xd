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

import java.util.HashMap;
import java.util.Map;
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
 * @author Gary Russell
 * @since 1.0
 *
 */
public class TriggerPlugin implements Plugin {

	public static final String BEAN_NAME_PREFIX = "trigger.";

	/**
	 * Trigger type enum
	 */
	private static enum TriggerType {
		cron,
		fixedDelay,
		fixedRate
	};


	private ConfigurableApplicationContext commonApplicationContext;

	/**
	 * Processes a new {@link Trigger} being added. Currently, it supports adding
	 * {@link CronTrigger}s. The {@link Trigger} is added to the common
	 * {@link ConfigurableApplicationContext}.
	 */
	@Override
	public void preProcessModule(Module module) {
		if (!TRIGGER.equals(module.getType())) {
			return;
		}
		Assert.notNull(commonApplicationContext,
				"The 'commonApplicationContext' property must not be null.");

		final BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition();
		Map<String, Trigger> triggers = new HashMap<String, Trigger>();
		if (module.getProperties().containsKey(TriggerType.cron.name())) {
			Trigger trigger = new CronTrigger(module.getProperties().getProperty(TriggerType.cron.name()));
			triggers.put(TriggerType.cron.name(), trigger);
		}
		if (module.getProperties().containsKey(TriggerType.fixedDelay.name())) {
			Trigger trigger = new PeriodicTrigger(Long.parseLong(module.getProperties().getProperty(TriggerType.fixedDelay.name())));
			triggers.put(TriggerType.fixedDelay.name(), trigger);
		}
		if (module.getProperties().containsKey(TriggerType.fixedRate.name())) {
			PeriodicTrigger trigger = new PeriodicTrigger(Long.parseLong(module.getProperties().getProperty(TriggerType.fixedRate.name())));
			trigger.setFixedRate(true);
			triggers.put(TriggerType.fixedRate.name(), trigger);
		}
		if (triggers.size() == 0) {
			throw new ResourceDefinitionException(
					"No valid trigger property. Expected one of: cron, fixedDelay or fixedRate");
		}
		else if (triggers.size() > 1) {
			throw new ResourceDefinitionException("Only one trigger property allowed, but received: "
					+ StringUtils.collectionToCommaDelimitedString(triggers.keySet()));
		}
		String group = module.getDeploymentMetadata().getGroup();
		commonApplicationContext.getBeanFactory().registerSingleton(BEAN_NAME_PREFIX + group, triggers.values().iterator().next());
		final BeanDefinitionAddingPostProcessor postProcessor = new BeanDefinitionAddingPostProcessor();
		postProcessor.addBeanDefinition(BEAN_NAME_PREFIX + group, builder.getBeanDefinition());
		this.commonApplicationContext.addBeanFactoryPostProcessor(postProcessor);
		configureProperties(module);
	}

	@Override
	public void postProcessModule(Module module) {
	}

	@Override
	public void removeModule(Module module) {
	}

	private void configureProperties(Module module) {
		Properties properties = new Properties();
		properties.setProperty("xd.stream.name", module.getDeploymentMetadata().getGroup());
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
