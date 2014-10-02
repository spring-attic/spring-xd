/*
 * Copyright 2013-2014 the original author or authors.
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

import static org.springframework.xd.module.options.spi.ModulePlaceholders.XD_JOB_NAME_KEY;

import java.util.Properties;

import org.springframework.core.io.ClassPathResource;
import org.springframework.util.Assert;
import org.springframework.xd.dirt.integration.bus.MessageBus;
import org.springframework.xd.dirt.plugins.AbstractJobPlugin;
import org.springframework.xd.module.core.Module;

/**
 * Plugin to enable the registration of jobs in a central registry.
 *
 * @author Michael Minella
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Glenn Renfro
 * @author Ilayaperumal Gopinathan
 * @author David Turanski
 * @since 1.0
 */
public class JobPlugin extends AbstractJobPlugin {

	private static final String REGISTRAR = PLUGIN_CONTEXT_CONFIG_ROOT + "job/job-module-beans.xml";

	public static final String JOB_PARAMETERS_KEY = "jobParameters";

	public JobPlugin(MessageBus messageBus) {
		super(messageBus);
	}

	private void configureProperties(Module module) {
		final Properties properties = new Properties();
		properties.setProperty(XD_JOB_NAME_KEY, module.getDescriptor().getGroup());
		module.addProperties(properties);
	}

	@Override
	public void preProcessModule(Module module) {
		Assert.notNull(module, "module cannot be null");
		module.addSource(new ClassPathResource(REGISTRAR));
		configureProperties(module);
	}

	@Override
	public void postProcessModule(Module module) {
		bindConsumerAndProducers(module);
	}

	@Override
	public int getOrder() {
		return HIGHEST_PRECEDENCE;
	}
}
