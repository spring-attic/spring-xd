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

package org.springframework.xd.dirt.config;

import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.integration.x.bus.AbstractTestMessageBus;
import org.springframework.xd.dirt.module.ModuleDeployer;
import org.springframework.xd.dirt.plugins.TestStreamPlugin;
import org.springframework.xd.dirt.plugins.stream.StreamPlugin;
import org.springframework.xd.dirt.server.SingleNodeApplication;


/**
 * Test class that helps injecting messageBus into {@link ModuleDeployer}'s common context via {@link TestStreamPlugin}.
 * 
 * @author Ilayaperumal Gopinathan
 */
public class TestMessageBusInjection {

	private static final String STREAM_PLUGIN_BEAN_ID = "streamPlugin";

	public static void injectMessageBus(SingleNodeApplication application, AbstractTestMessageBus testMessageBus) {
		ConfigurableApplicationContext containerContext = application.getContainerContext();
		StreamPlugin streamPlugin = containerContext.getBean(StreamPlugin.class);
		ModuleDeployer moduleDeployer = containerContext.getBean(ModuleDeployer.class);
		RootBeanDefinition bDefinition = new RootBeanDefinition(TestStreamPlugin.class);
		ConstructorArgumentValues constructorArgumentValues = new ConstructorArgumentValues();
		constructorArgumentValues.addIndexedArgumentValue(0, streamPlugin);
		constructorArgumentValues.addIndexedArgumentValue(1, testMessageBus);
		bDefinition.setConstructorArgumentValues(constructorArgumentValues);
		BeanDefinitionRegistry bdr = (BeanDefinitionRegistry) containerContext.getBeanFactory();
		bdr.removeBeanDefinition(STREAM_PLUGIN_BEAN_ID);
		bdr.registerBeanDefinition(STREAM_PLUGIN_BEAN_ID, bDefinition);
		moduleDeployer.onInit();
	}

}
