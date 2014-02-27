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

package org.springframework.xd.dirt.module;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.xd.dirt.util.ConfigLocations;

/**
 * 
 * @author David Turanski
 */
public class MessageBusInitializer implements SharedContextInitializer {

	private static final String CONTEXT_CONFIG_ROOT = ConfigLocations.XD_CONFIG_ROOT + "listeners/bus/";

	private static final String MESSAGE_BUS = CONTEXT_CONFIG_ROOT + "message-bus.xml";

	private static final String CODEC = CONTEXT_CONFIG_ROOT + "codec.xml";

	@Override
	public void onApplicationEvent(ApplicationPreparedEvent event) {
		ConfigurableApplicationContext context = event.getApplicationContext();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader((BeanDefinitionRegistry) context.getBeanFactory());
		reader.setEnvironment(context.getEnvironment());
		reader.loadBeanDefinitions(MESSAGE_BUS, CODEC);
	}

	@Override
	public int getOrder() {
		return Integer.MIN_VALUE;
	}

}
