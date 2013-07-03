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

import static org.springframework.xd.module.ModuleType.PROCESSOR;
import static org.springframework.xd.module.ModuleType.SINK;
import static org.springframework.xd.module.ModuleType.SOURCE;

import java.util.Properties;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.xd.dirt.container.DefaultContainer;
import org.springframework.xd.module.Module;
import org.springframework.xd.plugin.AbstractPlugin;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author David Turanski
 * @author Jennifer Hickey
 */
public class StreamPlugin extends AbstractPlugin {

	private static final String CONTEXT_CONFIG_ROOT = DefaultContainer.XD_CONFIG_ROOT
			+ "plugins/stream/";
	private static final String TAP_XML = CONTEXT_CONFIG_ROOT + "tap.xml";
	private static final String CHANNEL_REGISTRAR = CONTEXT_CONFIG_ROOT + "channel-registrar.xml";
	private static final String CHANNEL_REGISTRY = CONTEXT_CONFIG_ROOT + "channel-registry.xml";

	private static final String TAP = "tap";

	@Override
	public void processModule(Module module, String group, int index) {
		String type = module.getType();
		if ((SOURCE.equals(type) || PROCESSOR.equals(type) || SINK.equals(type)) && group != null) {
			addComponents(module, CHANNEL_REGISTRAR);
			this.configureProperties(module, group, String.valueOf(index));
		}
		if (TAP.equals(module.getName()) && SOURCE.equals(type)) {
			addComponents(module, TAP_XML);
		}
	}

	@Override
	public void postProcessSharedContext(ConfigurableApplicationContext context) {
		addBeanFactoryPostProcessor(context, CHANNEL_REGISTRY);
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
