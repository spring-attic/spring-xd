/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.xd.dirt.plugins.spark.streaming;

import java.util.Iterator;
import java.util.Properties;

import org.apache.spark.storage.StorageLevel;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.xd.dirt.integration.bus.MessageBus;
import org.springframework.xd.dirt.plugins.AbstractStreamPlugin;
import org.springframework.xd.dirt.plugins.stream.ModuleTypeConversionSupport;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.core.Module;
import org.springframework.xd.module.core.ModuleFactory;
import org.springframework.xd.module.spark.streaming.SparkStreamingDriverModule;
import org.springframework.xd.spark.streaming.SparkStreamingSupport;

/**
 * Plugin for Spark Streaming support. This plugin sets up the necessary beans for the spark streaming module
 * that connects to the underlying {@link MessageBus} to receive/send messages.
 *
 * @author Ilayaperumal Gopinathan
 * @author Mark Fisher
 * @since 1.1
 */
@SuppressWarnings("rawtypes")
public class SparkStreamingPlugin extends AbstractStreamPlugin {

	private static final String REDIS_CONNECTION_PROPERTY_PREFIX = "spring.redis";

	private static final String RABBIT_CONNECTION_PROPERTY_PREFIX = "spring.rabbitmq";

	private static final String MESSAGE_BUS_PROPERTY_PREFIX = "xd.messagebus.";

	@Autowired
	public SparkStreamingPlugin(MessageBus messageBus) {
		super(messageBus);
	}

	@Override
	public boolean supports(Module module) {
		String moduleExecutionFramework = module.getProperties().getProperty(ModuleFactory.MODULE_EXECUTION_FRAMEWORK_KEY);
		return (SparkStreamingSupport.MODULE_EXECUTION_FRAMEWORK.equals(moduleExecutionFramework));
	}

	@Override
	public void postProcessModule(Module module) {
		ConfigurableApplicationContext moduleContext = module.getApplicationContext();
		ConfigurableEnvironment env = moduleContext.getEnvironment();
		String transport = env.getProperty("XD_TRANSPORT");
		Properties messageBusProperties = getMessageBusProperties(module);
		Properties inboundModuleProperties = this.extractConsumerProducerProperties(module)[0];
		Properties outboundModuleProperties = this.extractConsumerProducerProperties(module)[1];
		String defaultStorageLevel = env.getProperty(SparkStreamingSupport.SPARK_STORAGE_LEVEL_PROP);
		StorageLevel configuredStorageLevel = StorageLevel.fromString(StringUtils.hasText(defaultStorageLevel) ?
				defaultStorageLevel : SparkStreamingSupport.SPARK_DEFAULT_STORAGE_LEVEL);
		String storageLevelFromModule = module.getProperties().getProperty(SparkStreamingSupport.SPARK_STORAGE_LEVEL_MODULE_OPTION);
		StorageLevel storageLevel = StringUtils.hasText(storageLevelFromModule) ?
				StorageLevel.fromString(storageLevelFromModule) : configuredStorageLevel;
		MessageBusReceiver receiver = null;
		if (transport.equals("local")) {
			SparkStreamingSupport processor;
			Properties sparkConfigs = null;
			try {
				processor = module.getComponent(SparkStreamingSupport.class);
				Assert.notNull(processor, "Problem getting the spark streaming module. Is the module context active?");
				sparkConfigs = SparkStreamingDriverModule.getSparkModuleProperties(processor);
			}
			catch (NoSuchBeanDefinitionException e) {
				throw new IllegalStateException("Either java or scala module should be present.");
			}
			String sparkMasterUrl = env.getProperty(SparkStreamingSupport.SPARK_MASTER_URL_PROP);
			if (sparkConfigs != null &&
					StringUtils.hasText(sparkConfigs.getProperty(SparkStreamingSupport.SPARK_MASTER_URL_PROP))) {
				sparkMasterUrl = sparkConfigs.getProperty(SparkStreamingSupport.SPARK_MASTER_URL_PROP);
			}
			Assert.notNull(sparkMasterUrl, "Spark Master URL must be set.");
			if (!sparkMasterUrl.startsWith("local")) {
				throw new IllegalStateException("Spark cluster mode must be 'local' for 'local' XD transport.");
			}
			LocalMessageBusHolder messageBusHolder = new LocalMessageBusHolder();
			LocalMessageBusHolder.set(module.getComponent(MessageBus.class));
			receiver = new MessageBusReceiver(messageBusHolder, storageLevel, messageBusProperties,
					inboundModuleProperties, ModuleTypeConversionSupport.getInputMimeType(module));
			if (module.getType().equals(ModuleType.processor)) {
				MessageBusSender sender = new MessageBusSender(messageBusHolder, getOutputChannelName(module),
						buildTapChannelName(module), messageBusProperties, outboundModuleProperties,
						ModuleTypeConversionSupport.getOutputMimeType(module), module.getProperties());
				ConfigurableBeanFactory beanFactory = module.getApplicationContext().getBeanFactory();
				beanFactory.registerSingleton("messageBusSender", sender);
			}
		}
		else {
			receiver = new MessageBusReceiver(storageLevel, messageBusProperties, inboundModuleProperties,
					ModuleTypeConversionSupport.getInputMimeType(module));
			if (module.getType().equals(ModuleType.processor)) {
				ConfigurableBeanFactory beanFactory = module.getApplicationContext().getBeanFactory();
				MessageBusSender sender = new MessageBusSender(getOutputChannelName(module), buildTapChannelName(module),
						messageBusProperties, outboundModuleProperties,
						ModuleTypeConversionSupport.getOutputMimeType(module), module.getProperties());
				beanFactory.registerSingleton("messageBusSender", sender);
			}
		}
		registerMessageBusReceiver(receiver, module);
	}

	/**
	 * Get the configured message bus properties for the given transport.
	 * @param module
	 * @return the message bus properties for the spark streaming module.
	 */
	private Properties getMessageBusProperties(Module module) {
		ConfigurableEnvironment env = module.getApplicationContext().getEnvironment();
		Properties busProperties = new Properties();
		busProperties.put("XD_TRANSPORT", env.getProperty("XD_TRANSPORT"));
		Iterator<PropertySource<?>> i = env.getPropertySources().iterator();
		while (i.hasNext()) {
			PropertySource<?> p = i.next();
			if (p instanceof EnumerablePropertySource) {
				for (String name : ((EnumerablePropertySource) p).getPropertyNames()) {
					if ((name.startsWith(REDIS_CONNECTION_PROPERTY_PREFIX)) ||
							name.startsWith(RABBIT_CONNECTION_PROPERTY_PREFIX) ||
							name.startsWith(MESSAGE_BUS_PROPERTY_PREFIX)) {
						busProperties.put(name, env.getProperty(name));
					}
				}
			}
		}
		return busProperties;
	}

	/**
	 * Register the messsage bus receiver.
	 * @param receiver the message bus receiver
	 * @param module the spark streaming module
	 */

	private void registerMessageBusReceiver(MessageBusReceiver receiver, Module module) {
		receiver.setInputChannelName(getInputChannelName(module));
		ConfigurableBeanFactory beanFactory = module.getApplicationContext().getBeanFactory();
		beanFactory.registerSingleton("messageBusReceiver", receiver);
	}

}
