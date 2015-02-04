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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.spark.storage.StorageLevel;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.xd.dirt.integration.bus.BusProperties;
import org.springframework.xd.dirt.integration.bus.ConnectionPropertyNames;
import org.springframework.xd.dirt.integration.bus.MessageBus;
import org.springframework.xd.dirt.integration.bus.MessageBusSupport;
import org.springframework.xd.dirt.plugins.AbstractStreamPlugin;
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
 */
@SuppressWarnings("rawtypes")
public class SparkStreamingPlugin extends AbstractStreamPlugin {

	@Autowired
	public SparkStreamingPlugin(MessageBus messageBus, ZooKeeperConnection zkConnection) {
		super(messageBus, zkConnection);
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
		Properties messageBusProperties = getMessageBusProperties(module, transport);
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
			if (sparkConfigs != null && StringUtils.hasText(sparkConfigs.getProperty(SparkStreamingSupport.SPARK_MASTER_URL_PROP))) {
				sparkMasterUrl = sparkConfigs.getProperty(SparkStreamingSupport.SPARK_MASTER_URL_PROP);
			}
			Assert.notNull(sparkMasterUrl, "Spark Master URL must be set.");
			if (!sparkMasterUrl.startsWith("local")) {
				throw new IllegalStateException("Spark cluster mode must be 'local' for 'local' XD transport.");
			}
			LocalMessageBusHolder messageBusHolder = new LocalMessageBusHolder();
			LocalMessageBusHolder.set(module.getComponent(MessageBus.class));
			receiver = new MessageBusReceiver(messageBusHolder, storageLevel, messageBusProperties, inboundModuleProperties);
			if (module.getType().equals(ModuleType.processor)) {
				ConfigurableBeanFactory beanFactory = module.getApplicationContext().getBeanFactory();
				MessageBusSender sender = new MessageBusSender(messageBusHolder,
						getOutputChannelName(module), messageBusProperties, outboundModuleProperties);
				beanFactory.registerSingleton("messageBusSender", sender);
			}
		}
		else {
			receiver = new MessageBusReceiver(storageLevel, messageBusProperties, inboundModuleProperties);
			if (module.getType().equals(ModuleType.processor)) {
				ConfigurableBeanFactory beanFactory = module.getApplicationContext().getBeanFactory();
				MessageBusSender sender = new MessageBusSender(getOutputChannelName(module),
						messageBusProperties, outboundModuleProperties);
				beanFactory.registerSingleton("messageBusSender", sender);
			}
		}
		registerMessageBusReceiver(receiver, module);
	}

	/**
	 * Get the configured message bus properties for the given transport.
	 * @param module
	 * @param transport
	 * @return the message bus properties for the spark streaming module.
	 */
	private Properties getMessageBusProperties(Module module, String transport) {
		ConfigurableEnvironment env = module.getApplicationContext().getEnvironment();
		Properties busProperties = new Properties();
		busProperties.put("XD_TRANSPORT", transport);
		if (!transport.equals("local")) {
			List<String> messageBusConnectionProperties = new ArrayList<String>();
			String connectionPropertiesClassName = transport.substring(0, 1).toUpperCase() + transport.substring(1) +
					ConnectionPropertyNames.class.getSimpleName();
			try {
				Class connectionPropertyNames = Class.forName(ConnectionPropertyNames.PACKAGE_NAME + connectionPropertiesClassName);
				messageBusConnectionProperties.addAll(Arrays.asList(((ConnectionPropertyNames) connectionPropertyNames.newInstance()).get()));
			}
			catch (ClassNotFoundException cnfe) {
				throw new RuntimeException(String.format("The transport %s must provide class %s", transport, connectionPropertiesClassName));
			}
			catch (ReflectiveOperationException roe) {
				throw new RuntimeException(roe);
			}
			for (String propertyKey : messageBusConnectionProperties) {
				String resolvedValue = env.resolvePlaceholders("${" + propertyKey + "}");
				busProperties.put(propertyKey, resolvedValue);
			}
		}
		List<String> messageBusPropertyKeys = new ArrayList<String>();
		Field[] propertyFields = BusProperties.class.getFields();
		for (Field f : propertyFields) {
			try {
				messageBusPropertyKeys.add((String) f.get(null));
			}
			catch (IllegalAccessException e) {
				// ignore the exception.
			}
		}
		String[] properties = ((MessageBusSupport) messageBus).getMessageBusSpecificProperties();
		messageBusPropertyKeys.addAll(Arrays.asList(properties));
		for (String key : messageBusPropertyKeys) {
			String propertyName = "xd.messagebus." + transport + ".default." + key;
			String resolvedValue = env.resolvePlaceholders("${" + propertyName + "}");
			if (!resolvedValue.contains("${")) {
				busProperties.put(propertyName, resolvedValue);
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
