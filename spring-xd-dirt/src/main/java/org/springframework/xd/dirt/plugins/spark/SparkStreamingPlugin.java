/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.xd.dirt.plugins.spark;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.autoconfigure.redis.RedisProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.xd.dirt.integration.bus.MessageBus;
import org.springframework.xd.dirt.module.spark.MessageBusReceiver;
import org.springframework.xd.dirt.module.spark.MessageBusSender;
import org.springframework.xd.dirt.plugins.stream.StreamPlugin;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.core.Module;

/**
 * Plugin for Spark Streaming support.
 *
 * @author Ilayaperumal Gopinathan
 */
public class SparkStreamingPlugin extends StreamPlugin {

	@Autowired
	public SparkStreamingPlugin(MessageBus messageBus, ZooKeeperConnection zkConnection) {
		super(messageBus, zkConnection);
	}

	@Override
	public boolean supports(Module module) {
		return (module.getName().contains("spark") && (module.getType().equals(ModuleType.processor) || module.getType().equals(ModuleType.sink)));
	}

	@Override
	public void postProcessModule(Module module) {
		ConfigurableApplicationContext moduleContext = module.getApplicationContext();
		ConfigurableBeanFactory beanFactory = moduleContext.getBeanFactory();
		Properties messageBusProperties = getMessageBusProperties(moduleContext.getParent());
		MessageBusReceiver receiver = new MessageBusReceiver(messageBusProperties);
		receiver.setInputChannelName(getInputChannelName(module));
		beanFactory.registerSingleton("messageBusReceiver", receiver);
		if (module.getType().equals(ModuleType.processor)) {
			MessageBusSender sender = new MessageBusSender(getOutputChannelName(module), messageBusProperties);
			beanFactory.registerSingleton("messageBusSender", sender);
		}
	}

	private Properties getMessageBusProperties(ApplicationContext moduleParentContext) {
		Properties properties = new Properties();
		String transport = moduleParentContext.getEnvironment().getProperty("XD_TRANSPORT");
		properties.setProperty("XD_TRANSPORT", transport);
		if (transport.equals("rabbit")) {
			RabbitProperties rabbitProperties = moduleParentContext.getBean(RabbitProperties.class);
			updateRabbitProperties(rabbitProperties, properties, "spring.rabbitmq.");
		}
		else if (transport.equals("redis")) {
			RedisProperties redisProperties = moduleParentContext.getBean(RedisProperties.class);
			updateRedisProperties(redisProperties, properties, "spring.redis.");
		}
		return properties;
	}

	private void updateRabbitProperties(RabbitProperties obj, Properties properties, String propertyPrefix) {
		try {
			for (PropertyDescriptor pd : Introspector.getBeanInfo(RabbitProperties.class).getPropertyDescriptors()) {
				if (pd.getReadMethod() != null && !"class".equals(pd.getName())) {
						properties.setProperty(propertyPrefix + pd.getName(), String.valueOf(pd.getReadMethod().invoke((obj))));
				}
			}
		}
		catch (Exception e) {
			//todo
		}
	}

	private void updateRedisProperties(RedisProperties obj, Properties properties, String propertyPrefix) {
		try {
			for (PropertyDescriptor pd : Introspector.getBeanInfo(RedisProperties.class).getPropertyDescriptors()) {
				if (pd.getReadMethod() != null && !"class".equals(pd.getName())) {
					if (pd.getReadMethod() != null && !"class".equals(pd.getName())) {
						properties.setProperty(propertyPrefix + pd.getName(), String.valueOf(pd.getReadMethod().invoke((obj))));
					}
				}
			}
		}
		catch (Exception e) {
			//todo
		}
	}

}
