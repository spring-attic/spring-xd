/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.module.spark.streaming;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.spark.SparkConf;
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
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.Assert;
import org.springframework.util.SocketUtils;
import org.springframework.util.StringUtils;
import org.springframework.xd.module.ModuleDeploymentProperties;
import org.springframework.xd.module.ModuleDescriptor;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.core.ResourceConfiguredModule;
import org.springframework.xd.module.options.ModuleOptions;
import org.springframework.xd.spark.streaming.SparkConfig;
import org.springframework.xd.spark.streaming.SparkMessageSender;
import org.springframework.xd.spark.streaming.SparkStreamingSupport;
import org.springframework.xd.spark.streaming.java.ModuleExecutor;
import org.springframework.xd.spark.streaming.java.Processor;

/**
 * The driver that adapts an implementation of either {@link org.springframework.xd.spark.streaming.java.Processor}
 * or {@link org.springframework.xd.spark.streaming.scala.Processor} to be executed as an XD module.
 *
 * @author Ilayaperumal Gopinathan
 * @author Mark Fisher
 */
@SuppressWarnings("rawtypes")
public class SparkStreamingDriverModule extends ResourceConfiguredModule {

	public static final String MESSAGE_BUS_JARS_LOCATION = "file:${XD_HOME}/lib/messagebus/${XD_TRANSPORT}/*.jar";

	/**
	 * Logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(SparkStreamingDriverModule.class);

	private PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

	private JavaStreamingContext javaStreamingContext;

	private final CountDownLatch receiverStartLatch = new CountDownLatch(1);

	private final AtomicBoolean receiverStartSuccess = new AtomicBoolean();


	/**
	 * Create a SparkDriver
	 *
	 * @param descriptor
	 * @param deploymentProperties
	 * @param classLoader
	 * @param moduleOptions
	 */
	public SparkStreamingDriverModule(ModuleDescriptor descriptor, ModuleDeploymentProperties deploymentProperties,
			ClassLoader classLoader, ModuleOptions moduleOptions) {
		super(descriptor, deploymentProperties, classLoader, moduleOptions);
	}


	@Override
	public void start() {
		super.start();
		logger.info("Starting SparkDriver");
		SparkStreamingSupport processor;
		Properties sparkConfigs = null;
		try {
			processor = getComponent(SparkStreamingSupport.class);
			Assert.notNull(processor, "Problem getting the spark streaming module. Is the module context active?");
			sparkConfigs = getSparkModuleProperties(processor);
		}
		catch (NoSuchBeanDefinitionException e) {
			throw new IllegalStateException("Either java or scala module should be present.");
		}
		startSparkStreamingContext(sparkConfigs, processor);
	}

	/**
	 * Start spark streaming context for the given streaming processor.
	 *
	 * @param sparkConfigs the spark configuration properties
	 * @param sparkStreamingSupport the underlying processor implementation
	 */
	private void startSparkStreamingContext(Properties sparkConfigs,
			final SparkStreamingSupport sparkStreamingSupport) {
		final Receiver receiver = getComponent(Receiver.class);
		Environment env = this.getApplicationContext().getEnvironment();
		String masterURL = env.getProperty(SparkStreamingSupport.SPARK_MASTER_URL_PROP,
				SparkStreamingSupport.SPARK_DEFAULT_MASTER_URL);
		final SparkConf sparkConf = setupSparkConf(masterURL, sparkConfigs);
		final String batchInterval = env.getProperty(SparkStreamingSupport.SPARK_STREAMING_BATCH_INTERVAL_MODULE_OPTION,
				env.getProperty(SparkStreamingSupport.SPARK_STREAMING_BATCH_INTERVAL_PROP,
						SparkStreamingSupport.SPARK_STREAMING_DEFAULT_BATCH_INTERVAL));
		final SparkStreamingListener streamingListener = new SparkStreamingListener();

		final SparkMessageSender sender =
				(this.getType().equals(ModuleType.processor)) ? getComponent(SparkMessageSender.class) : null;
		final StreamingContext streamingContext = new StreamingContext(sparkConf, new Duration(Long.valueOf(batchInterval)));
		streamingContext.addStreamingListener(streamingListener);
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			@Override
			@SuppressWarnings("unchecked")
			public void run() {
				try {
					javaStreamingContext = new JavaStreamingContext(streamingContext);
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
			boolean started = receiverStartLatch.await(30, TimeUnit.SECONDS);
			if (!started) {
				logger.warn("Deployment timed out when deploying Spark Streaming module " + sparkStreamingSupport);
			}
			if (!receiverStartSuccess.get()) {
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
	private SparkConf setupSparkConf(String masterURL, Properties sparkConfigs) {
		SparkConf sparkConf = new SparkConf()
				// Set spark UI port to random available port to support multiple spark modules on the same host.
				.set("spark.ui.port", String.valueOf(SocketUtils.findAvailableTcpPort()))
				// Set the cores max so that multiple (at least a few) spark modules can be deployed on the same host.
				.set("spark.cores.max", "3")
				.setMaster(masterURL)
				.setAppName(getDescriptor().getGroup() + "-" + getDescriptor().getModuleLabel());
		if (sparkConfigs != null) {
			for (String property : sparkConfigs.stringPropertyNames()) {
				sparkConf.set(property, sparkConfigs.getProperty(property));
			}
		}
		List<String> sparkJars = new ArrayList<String>();
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
		sparkJars.addAll(getApplicationJars());
		sparkConf.setJars(sparkJars.toArray(new String[sparkJars.size()]));
		return sparkConf;
	}

	/**
	 * Retrieve spark configuration properties from the {@link org.springframework.xd.spark.streaming.java.Processor} implementation.
	 * This method uses {@link SparkConfig} annotation to derive the {@link Properties} returned
	 * from the annotated methods.
	 *
	 * @param processor the spark streaming processor
	 * @return the spark configuration properties (if defined) or empty properties
	 */
	public static Properties getSparkModuleProperties(SparkStreamingSupport processor) {
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
	 * Get the list of jars that this spark module requires.
	 *
	 * @return the list of spark application jars
	 */
	private List<String> getApplicationJars() {
		// Get jars from module classpath
		URLClassLoader classLoader = (URLClassLoader) this.getClassLoader();
		List<String> jars = new ArrayList<String>();
		for (URL url : classLoader.getURLs()) {
			String file = url.getFile().split("\\!", 2)[0];
			if (file.endsWith(".jar")) {
				jars.add(file);
			}
		}
		// Get message bus libraries
		Environment env = this.getApplicationContext().getEnvironment();
		String jarsLocation = env.resolvePlaceholders(MESSAGE_BUS_JARS_LOCATION);
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
		URLClassLoader parentClassLoader = (URLClassLoader) this.getClassLoader().getParent();
		URL[] urls = parentClassLoader.getURLs();
		for (URL url : urls) {
			String file = url.getFile().split("\\!", 2)[0];
			//if (file.endsWith(".jar")) {
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
					file.contains("kryo"))) {
				jars.add(file);
			}
		}
		return jars;
	}

	@Override
	public void stop() {
		logger.info("stopping SparkDriver");
		try {
			if (javaStreamingContext != null) {
				try {
					javaStreamingContext.stop(true, false);
				}
				catch(Exception e) {
					logger.warn("Error while stopping streaming context "+ e);
				}
			}
			super.stop();
		}
		catch (Exception e) {
			logger.warn("Exception when stopping the spark module " + e);
		}
	}

	@Override
	public boolean shouldBind() {
		return false;
	}

	/**
	 * StreamingListener that processes spark {@link org.apache.spark.streaming.scheduler.StreamingListener} events.
	 */
	private class SparkStreamingListener implements StreamingListener {

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
