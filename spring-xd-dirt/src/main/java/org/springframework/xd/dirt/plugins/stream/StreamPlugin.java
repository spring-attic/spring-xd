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

import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.integration.x.bus.MessageBus;
import org.springframework.integration.x.bus.MessageBusAwareRouterBeanPostProcessor;
import org.springframework.xd.dirt.plugins.AbstractStreamPlugin;
import org.springframework.xd.module.DeploymentMetadata;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.core.Module;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author David Turanski
 * @author Jennifer Hickey
 * @author Glenn Renfro
 * @author Ilayaperumal Gopinathan
 */
public class StreamPlugin extends AbstractStreamPlugin {

	public StreamPlugin(MessageBus messageBus) {
		super(messageBus);
	}

	@Override
	public void preProcessModule(Module module) {
		DeploymentMetadata md = module.getDeploymentMetadata();
		Properties properties = new Properties();
		properties.setProperty("xd.stream.name", md.getGroup());
		properties.setProperty("xd.module.index", String.valueOf(md.getIndex()));
		module.addProperties(properties);
		if (module.getType() == ModuleType.sink) {
			module.addListener(new ApplicationListener<ApplicationPreparedEvent>() {

				@Override
				public void onApplicationEvent(ApplicationPreparedEvent event) {
					MessageBusAwareRouterBeanPostProcessor bpp = new MessageBusAwareRouterBeanPostProcessor(messageBus);
					bpp.setBeanFactory(event.getApplicationContext());
					event.getApplicationContext().getBeanFactory().registerSingleton(
							"messageBusAwareRouterBeanPostProcessor", bpp);
				}

			});
		}
	}

	@Override
	public void postProcessModule(Module module) {
		bindConsumerAndProducers(module);
	}

	@Override
	public void beforeShutdown(Module module) {
		unbindConsumerAndProducers(module);
	}

	@Override
	public int getOrder() {
		return HIGHEST_PRECEDENCE;
	}
}
