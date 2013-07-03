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

import static org.springframework.xd.module.ModuleType.JOB;

import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.xd.dirt.container.DefaultContainer;
import org.springframework.xd.module.Module;
import org.springframework.xd.plugin.AbstractPlugin;

/**
 * Plugin to enable the registration of jobs in a central registry.
 *
 * @author Michael Minella
 * @author Gunnar Hillert
 * @since 1.0
 *
 */
public class JobPlugin extends AbstractPlugin  {

	private final Log logger = LogFactory.getLog(getClass());

	private static final String CONTEXT_CONFIG_ROOT = DefaultContainer.XD_CONFIG_ROOT
			+ "plugins/job/";
	private static final String REGISTRAR_WITH_TRIGGER_REF =
			CONTEXT_CONFIG_ROOT + "registrar-with-trigger-ref.xml";
	private static final String REGISTRAR_WITH_CRON =
			CONTEXT_CONFIG_ROOT + "registrar-with-cron.xml";
	private static final String REGISTRAR = CONTEXT_CONFIG_ROOT + "registrar.xml";
	private static final String COMMON_XML = CONTEXT_CONFIG_ROOT + "common.xml";
	private static final String TRIGGER = "trigger";
	private static final String CRON = "cron";

	/**
	 * Process the {@link Module} and add the Application Context resources
	 * necessary to setup the Batch Job.
	 */
	@Override
	public void processModule(Module module, String group, int index) {
		if (!JOB.equals(module.getType())) {
			return;
		}

		if (module.getProperties().containsKey(TRIGGER)) {
			addComponents(module, REGISTRAR_WITH_TRIGGER_REF);
		}
		else if (module.getProperties().containsKey("cron")) {
			addComponents(module, REGISTRAR_WITH_CRON);
		}
		else if (module.getProperties().containsKey("fixed-delay")) {
			module.addComponents(new ClassPathResource(CONTEXT_CONFIG_ROOT + "registrar-with-fixed-delay.xml"));
		}
		else {
			addComponents(module, REGISTRAR);
		}

		configureProperties(module, group);
	}

	@Override
	public void removeModule(Module module, String group, int index) {
	}

	@Override
	public void postProcessSharedContext(ConfigurableApplicationContext context) {
		addBeanFactoryPostProcessor(context, COMMON_XML);
	}

	private void configureProperties(Module module, String group) {
		final Properties properties = new Properties();
		properties.setProperty("xd.stream.name", group);

		if (module.getProperties().containsKey(TRIGGER) || module.getProperties().containsKey(CRON)) {
			properties.setProperty("xd.trigger.execute_on_startup", "false");
		}
		else {
			properties.setProperty("xd.trigger.execute_on_startup", "true");
		}

		module.addProperties(properties);

		if (logger.isInfoEnabled()) {
			logger.info("Configuring module with the following properties: " + properties.toString());
		}

	}
}
