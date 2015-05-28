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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.FilenameUtils;
import org.apache.spark.SparkConf;
import org.apache.spark.storage.StorageLevel;
import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.StreamingContext;
import org.apache.spark.streaming.api.java.JavaReceiverInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.dstream.ReceiverInputDStream;
import org.apache.spark.streaming.receiver.Receiver;
import org.apache.spark.streaming.scheduler.StreamingListener;
import org.apache.spark.streaming.scheduler.StreamingListenerBatchCompleted;
import org.apache.spark.streaming.scheduler.StreamingListenerBatchStarted;
import org.apache.spark.streaming.scheduler.StreamingListenerBatchSubmitted;
import org.apache.spark.streaming.scheduler.StreamingListenerReceiverError;
import org.apache.spark.streaming.scheduler.StreamingListenerReceiverStarted;
import org.apache.spark.streaming.scheduler.StreamingListenerReceiverStopped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.Assert;
import org.springframework.util.SocketUtils;
import org.springframework.util.StringUtils;
import org.springframework.xd.dirt.integration.bus.MessageBus;
import org.springframework.xd.dirt.plugins.AbstractStreamPlugin;
import org.springframework.xd.dirt.plugins.stream.ModuleTypeConversionSupport;
import org.springframework.xd.dirt.server.MessageBusClassLoaderFactory;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.core.Module;
import org.springframework.xd.module.core.ModuleFactory;
import org.springframework.xd.module.core.SimpleModule;
import org.springframework.xd.spark.streaming.SparkConfig;
import org.springframework.xd.spark.streaming.SparkMessageSender;
import org.springframework.xd.spark.streaming.SparkStreamingSupport;
import org.springframework.xd.spark.streaming.java.ModuleExecutor;
import org.springframework.xd.spark.streaming.java.Processor;

/**
 * Plugin for Spark Streaming support. This plugin sets up the necessary beans for the spark streaming module
 * that connects to the underlying {@link MessageBus} to receive/send messages.
 *
 * @author Ilayaperumal Gopinathan
 * @author Mark Fisher
 * @author Eric Bottard
 * @since 1.1
 */
@SuppressWarnings("rawtypes")
public class SparkStreamingPlugin extends AbstractStreamPlugin {
	/**
	 * Logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(SparkStreamingPlugin.class);

	private static final String REDIS_CONNECTION_PROPERTY_PREFIX = "spring.redis";

	private static final String RABBIT_CONNECTION_PROPERTY_PREFIX = "spring.rabbitmq";

	private static final String MESSAGE_BUS_PROPERTY_PREFIX = "xd.messagebus.";

	private PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

	private Map<Module, JavaStreamingContext> streamingContexts = new HashMap<>();

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
				sparkConfigs = getSparkModuleProperties(processor);
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


		// This used to be in SSDModule.start
		try {
			SparkStreamingSupport processor = module.getComponent(SparkStreamingSupport.class);
			Assert.notNull(processor, "Problem getting the spark streaming module. Is the module context active?");
			Properties sparkConfigs = getSparkModuleProperties(processor);
			startSparkStreamingContext(sparkConfigs, processor, module);
		}
		catch (NoSuchBeanDefinitionException e) {
			throw new IllegalStateException("Either java or scala module should be present.");
		}

	}

	@Override
	public void beforeShutdown(Module module) {
		super.beforeShutdown(module);

		logger.info("stopping SparkDriver");
		try {
			try {
				streamingContexts.get(module).stop(true, false);
			}
			catch(Exception e) {
				logger.warn("Error while stopping streaming context "+ e);
			}
		}
		catch (Exception e) {
			logger.warn("Exception when stopping the spark module " + e);
		}

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

	/**
	 * Retrieve spark configuration properties from the {@link org.springframework.xd.spark.streaming.java.Processor} implementation.
	 * This method uses {@link SparkConfig} annotation to derive the {@link Properties} returned
	 * from the annotated methods.
	 *
	 * @param processor the spark streaming processor
	 * @return the spark configuration properties (if defined) or empty properties
	 */
	private Properties getSparkModuleProperties(SparkStreamingSupport processor) {
		Properties sparkConfigs = new Properties();
		Method[] methods = processor.getClass().getDeclaredMethods();
		for (Method method : methods) {
			SparkConfig sparkConfig = method.getAnnotation(SparkConfig.class);
			if (sparkConfig != null) {
				try {
					if (method.getReturnType().equals(Properties.class)) {
						sparkConfigs.putAll((Properties) method.invoke(processor));
					}
					else {
						logger.warn("@SparkConfig annotated method should return java.util.Properties type. " +
								"Ignoring the method " + method.getName());
					}
				}
				catch (InvocationTargetException ise) {
					// ignore.
				}
				catch (IllegalAccessException ise) {
					// ignore.
				}
			}
		}
		return sparkConfigs;
	}

	/**
	 * Start spark streaming context for the given streaming processor.
	 *
	 * @param sparkConfigs the spark configuration properties
	 * @param sparkStreamingSupport the underlying processor implementation
	 */
	private void startSparkStreamingContext(Properties sparkConfigs,
											final SparkStreamingSupport sparkStreamingSupport,
											final Module module) {
		final Receiver receiver = module.getComponent(Receiver.class);
		Environment env = this.getApplicationContext().getEnvironment();
		String masterURL = env.getProperty(SparkStreamingSupport.SPARK_MASTER_URL_PROP,
				SparkStreamingSupport.SPARK_DEFAULT_MASTER_URL);
		final SparkConf sparkConf = setupSparkConf(module, masterURL, sparkConfigs);
		final String batchInterval = env.getProperty(SparkStreamingSupport.SPARK_STREAMING_BATCH_INTERVAL_MODULE_OPTION,
				env.getProperty(SparkStreamingSupport.SPARK_STREAMING_BATCH_INTERVAL_PROP,
						SparkStreamingSupport.SPARK_STREAMING_DEFAULT_BATCH_INTERVAL));
		final SparkStreamingListener streamingListener = new SparkStreamingListener();

		final SparkMessageSender sender =
				(module.getType()==ModuleType.processor) ? module.getComponent(SparkMessageSender.class) : null;
		final StreamingContext streamingContext = new StreamingContext(sparkConf, new Duration(Long.valueOf(batchInterval)));
		streamingContext.addStreamingListener(streamingListener);
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			@Override
			@SuppressWarnings("unchecked")
			public void run() {
				try {
					JavaStreamingContext javaStreamingContext = new JavaStreamingContext(streamingContext);
					streamingContexts.put(module, javaStreamingContext);
					JavaReceiverInputDStream javaInputDStream = javaStreamingContext.receiverStream(receiver);
					if (sparkStreamingSupport instanceof Processor) {
						new ModuleExecutor().execute(javaInputDStream, (Processor) sparkStreamingSupport, sender);
					}
					if (sparkStreamingSupport instanceof org.springframework.xd.spark.streaming.scala.Processor) {
						ReceiverInputDStream receiverInput = javaInputDStream.receiverInputDStream();
						new org.springframework.xd.spark.streaming.scala.ModuleExecutor().execute(receiverInput,
								(org.springframework.xd.spark.streaming.scala.Processor) sparkStreamingSupport, sender);
					}
					javaStreamingContext.start();
					javaStreamingContext.awaitTermination();
				}
				catch (Exception e) {
					throw new IllegalStateException("Exception when running Spark Streaming application.", e);
				}
			}
		});
		try {
			boolean started = streamingListener.receiverStartLatch.await(30, TimeUnit.SECONDS);
			if (!started) {
				logger.warn("Deployment timed out when deploying Spark Streaming module " + sparkStreamingSupport);
			}
			if (!streamingListener.receiverStartSuccess.get()) {
				throw new IllegalStateException("Failed to start Spark Streaming Receiver");
			}
		}
		catch (InterruptedException ie) {
			throw new RuntimeException(ie);
		}
	}

	/**
	 * Setup {@link org.apache.spark.SparkConf} for the given spark configuration properties.
	 *
	 * @param masterURL the spark cluster master URL
	 * @param sparkConfigs the spark configuration properties
	 * @return SparkConf for this spark streaming module
	 */
	private SparkConf setupSparkConf(Module module, String masterURL, Properties sparkConfigs) {
		SparkConf sparkConf = new SparkConf()
				// Set spark UI port to random available port to support multiple spark modules on the same host.
				.set("spark.ui.port", String.valueOf(SocketUtils.findAvailableTcpPort()))
						// Set the cores max so that multiple (at least a few) spark modules can be deployed on the same host.
				.set("spark.cores.max", "3")
				.setMaster(masterURL)
				.setAppName(module.getDescriptor().getGroup() + "-" + module.getDescriptor().getModuleLabel());
		if (sparkConfigs != null) {
			for (String property : sparkConfigs.stringPropertyNames()) {
				sparkConf.set(property, sparkConfigs.getProperty(property));
			}
		}
		List<String> sparkJars = new ArrayList<>();
		// Add jars from spark.jars (if any) set from spark module.
		try {
			String jarsFromConf = sparkConf.get("spark.jars");
			if (StringUtils.hasText(jarsFromConf)) {
				sparkJars.addAll(Arrays.asList(jarsFromConf.split("\\s*,\\s*")));
			}
		}
		catch (NoSuchElementException e) {
			// should ignore
		}
		sparkJars.addAll(getApplicationJars(module));
		sparkConf.setJars(sparkJars.toArray(new String[sparkJars.size()]));
		return sparkConf;
	}

	/**
	 * Get the list of jars that this spark module requires.
	 *
	 * @return the list of spark application jars
	 */
	private List<String> getApplicationJars(Module module) {
		// Get jars from module classpath
		URLClassLoader classLoader = (URLClassLoader) ((SimpleModule)module).getClassLoader();
		List<String> jars = new ArrayList<String>();
		for (URL url : classLoader.getURLs()) {
			String file = url.getFile().split("\\!", 2)[0];
			if (file.endsWith(".jar")) {
				jars.add(file);
			}
		}
		// Get message bus libraries
		Environment env = this.getApplicationContext().getEnvironment();
		String jarsLocation = env.resolvePlaceholders(MessageBusClassLoaderFactory.MESSAGE_BUS_JARS_LOCATION);
		try {
			Resource[] resources = resolver.getResources(jarsLocation);
			for (Resource resource : resources) {
				URL url = resource.getURL();
				jars.add(url.getFile());
			}
		}
		catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
		// Get necessary dependencies from XD DIRT.
		URLClassLoader parentClassLoader = (URLClassLoader) classLoader.getParent();
		URL[] urls = parentClassLoader.getURLs();
		for (URL url : urls) {
			String file = FilenameUtils.getName(url.getFile());
			String fileToAdd = url.getFile().split("\\!", 2)[0];
			if (file.endsWith(".jar") && (// Add spark jars
					file.contains("spark") ||
							// Add SpringXD dependencies
							file.contains("spring-xd-") ||
							// Add Spring dependencies
							file.contains("spring-core") ||
							file.contains("spring-integration-core") ||
							file.contains("spring-beans") ||
							file.contains("spring-context") ||
							file.contains("spring-boot") ||
							file.contains("spring-aop") ||
							file.contains("spring-expression") ||
							file.contains("spring-messaging") ||
							file.contains("spring-retry") ||
							file.contains("spring-tx") ||
							file.contains("spring-data-commons") ||
							file.contains("spring-data-redis") ||
							file.contains("commons-pool") ||
							file.contains("jedis") ||
							// Add codec dependency
							file.contains("kryo") ||
							file.contains("gs-collections"))) {
				jars.add(fileToAdd);
			}
		}
		return jars;
	}

	/**
	 * StreamingListener that processes spark {@link org.apache.spark.streaming.scheduler.StreamingListener} events.
	 */
	private static class SparkStreamingListener implements StreamingListener {
		private final CountDownLatch receiverStartLatch = new CountDownLatch(1);

		private final AtomicBoolean receiverStartSuccess = new AtomicBoolean();

		@Override
		/** Called when a receiver has been started */
		public void onReceiverStarted(StreamingListenerReceiverStarted started) {
			logger.info("Spark streaming receiver started " + started.receiverInfo());
			receiverStartSuccess.set(true);
			receiverStartLatch.countDown();
		}

		@Override
		/** Called when a receiver has reported an error */
		public void onReceiverError(StreamingListenerReceiverError receiverError) {
			logger.info("Error starting spark streaming receiver " + receiverError.receiverInfo());
			receiverStartSuccess.set(false);
			receiverStartLatch.countDown();
		}

		@Override
		/** Called when a receiver has been stopped */
		public void onReceiverStopped(StreamingListenerReceiverStopped receiverStopped) {
			logger.info("Spark streaming receiver stopped " + receiverStopped.receiverInfo());
		}

		/** Called when a batch of jobs has been submitted for processing. */
		public void onBatchSubmitted(StreamingListenerBatchSubmitted batchSubmitted) {
		}

		/** Called when processing of a batch of jobs has started.  */
		public void onBatchStarted(StreamingListenerBatchStarted batchStarted) {
		}

		/** Called when processing of a batch of jobs has completed. */
		public void onBatchCompleted(StreamingListenerBatchCompleted batchCompleted) {
		}
	}

}
