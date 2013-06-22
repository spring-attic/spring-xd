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

package org.springframework.xd.dirt.plugins.stream;

import java.util.Properties;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.xd.dirt.container.DefaultContainer;
import org.springframework.xd.dirt.plugins.BeanDefinitionAddingPostProcessor;
import org.springframework.xd.module.Module;
import org.springframework.xd.module.Plugin;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author David Turanski
 * @author Jennifer Hickey
 */
public class StreamPlugin implements Plugin {

	private static final String CONTEXT_CONFIG_ROOT = DefaultContainer.XD_CONFIG_ROOT
			+ "plugins/stream/";

	@Override
	public void processModule(Module module, String group, int index) {
		String type = module.getType();
		if (("source".equals(type) || "processor".equals(type) || "sink".equals(type)) && group != null) {
			module.addComponents(new ClassPathResource(CONTEXT_CONFIG_ROOT + "channel-registrar.xml"));
			this.configureProperties(module, group, String.valueOf(index));
		}
		if ("tap".equals(module.getName()) && "source".equals(type)) {
			module.addComponents(new ClassPathResource(CONTEXT_CONFIG_ROOT + "tap.xml"));
		}
	}

	@Override
	public void postProcessSharedContext(ConfigurableApplicationContext context) {
		context.addBeanFactoryPostProcessor(new BeanDefinitionAddingPostProcessor(new ClassPathResource(
				CONTEXT_CONFIG_ROOT + "channel-registry.xml")));
	}

	@Override
	public void removeModule(Module module, String group, int index) {
	}

	private void configureProperties(Module module, String group, String index) {
		Properties properties = new Properties();
		properties.setProperty("xd.stream.name", group);
		properties.setProperty("xd.module.index", index);
		module.addProperties(properties);
	}

}
