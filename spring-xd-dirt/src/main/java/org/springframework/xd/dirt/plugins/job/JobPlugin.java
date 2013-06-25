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
package org.springframework.xd.dirt.plugins.job;

import java.util.Properties;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.xd.dirt.container.DefaultContainer;
import org.springframework.xd.dirt.plugins.BeanDefinitionAddingPostProcessor;
import org.springframework.xd.module.Module;
import org.springframework.xd.module.Plugin;

/**
 * Plugin to enable the registration of jobs in a central registry
 *
 * @author Michael Minella
 * @since 1.0
 *
 */
public class JobPlugin implements Plugin {

	private static final String CONTEXT_CONFIG_ROOT = DefaultContainer.XD_CONFIG_ROOT
			+ "plugins/job/";

	@Override
	public void processModule(Module module, String group, int index) {
		module.addComponents(new ClassPathResource(CONTEXT_CONFIG_ROOT + "registrar.xml"));
		configureProperties(module, group);
	}

	@Override
	public void removeModule(Module module, String group, int index) {
	}

	@Override
	public void postProcessSharedContext(ConfigurableApplicationContext context) {
		context.addBeanFactoryPostProcessor(new BeanDefinitionAddingPostProcessor(new ClassPathResource(CONTEXT_CONFIG_ROOT + "common.xml")));
	}

	private void configureProperties(Module module, String group) {
		Properties properties = new Properties();
		properties.setProperty("xd.stream.name", group);
		module.addProperties(properties);
	}
}
