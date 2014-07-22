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

package org.springframework.xd.dirt.plugins.stream;

import static org.springframework.xd.module.options.spi.ModulePlaceholders.XD_STREAM_NAME_KEY;

import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.util.Assert;
import org.springframework.xd.dirt.integration.bus.MessageBus;
import org.springframework.xd.dirt.integration.bus.MessageBusAwareRouterBeanPostProcessor;
import org.springframework.xd.dirt.plugins.AbstractStreamPlugin;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;
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

	@Autowired
	public StreamPlugin(MessageBus messageBus, ZooKeeperConnection zkConnection) {
		super(messageBus, zkConnection);
		Assert.notNull(zkConnection, "ZooKeeperConnection must not be null");
	}

	@Override
	public void preProcessModule(final Module module) {
		Properties properties = new Properties();
		properties.setProperty(XD_STREAM_NAME_KEY, module.getDescriptor().getGroup());
		module.addProperties(properties);
		if (module.getType() == ModuleType.sink) {
			module.addListener(new ApplicationListener<ApplicationPreparedEvent>() {

				@Override
				public void onApplicationEvent(ApplicationPreparedEvent event) {
					Properties producerProperties = extractConsumerProducerProperties(module)[1];
					MessageBusAwareRouterBeanPostProcessor bpp =
							new MessageBusAwareRouterBeanPostProcessor(messageBus, producerProperties);
					bpp.setBeanFactory(event.getApplicationContext().getBeanFactory());
					event.getApplicationContext().getBeanFactory().addBeanPostProcessor(bpp);
				}

			});
		}
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
