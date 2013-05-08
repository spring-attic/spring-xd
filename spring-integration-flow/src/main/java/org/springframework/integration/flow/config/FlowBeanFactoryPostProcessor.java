/*
 * Copyright 2002-2013 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.integration.flow.config;

import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.integration.flow.Flow;
import org.springframework.integration.flow.FlowChannelRegistry;
import org.springframework.integration.flow.FlowPlugin;

/**
 * @author David Turanski
 * @since 3.0
 *
 */
public class FlowBeanFactoryPostProcessor implements BeanFactoryPostProcessor, ApplicationContextAware {

	private ApplicationContext applicationContext;

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		FlowChannelRegistry channelRegistry = new FlowChannelRegistry();
		channelRegistry.setApplicationContext(this.applicationContext);
		try {
			channelRegistry.afterPropertiesSet();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		FlowPlugin plugin = new FlowPlugin(channelRegistry);

		Map<String, Flow> flows = beanFactory.getBeansOfType(Flow.class);
		for (Entry<String, Flow> entry : flows.entrySet()) {
			plugin.processModule(entry.getValue());
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

}
