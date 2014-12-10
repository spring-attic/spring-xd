/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.xd.module.spark;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import org.apache.spark.SparkConf;
import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.receiver.Receiver;
import org.springframework.core.env.Environment;
import org.springframework.xd.module.ModuleDeploymentProperties;
import org.springframework.xd.module.ModuleDescriptor;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.core.ResourceConfiguredModule;
import org.springframework.xd.module.options.ModuleOptions;

/**
 * The driver that adapts an implementation of {@link SparkModule} to be executed as an XD module.
 *
 * @author Ilayaperumal Gopinathan
 * @author Mark Fisher
 */
public class SparkDriver extends ResourceConfiguredModule {

	private static final String SPARK_MASTER_URL = "spark://localhost:7077";

	private static final String SPARK_STREAMING_BATCH_INTERVAL = "2000";

	private JavaStreamingContext streamingContext;

	/**
	 * Create a SparkDriver
	 *
	 * @param descriptor
	 * @param deploymentProperties
	 */
	public SparkDriver(ModuleDescriptor descriptor, ModuleDeploymentProperties deploymentProperties,
			ClassLoader classLoader, ModuleOptions moduleOptions) {
		super(descriptor, deploymentProperties, classLoader, moduleOptions);
	}

	@Override
	public void initialize() {
		super.initialize();
		URLClassLoader classLoader = (URLClassLoader) this.getClassLoader();
		List<String> jars = new ArrayList<String>();
		for (URL url : classLoader.getURLs()) {
			String file = url.getFile().split("\\!", 2)[0];
			if (file.endsWith(".jar")) {
				jars.add(file);
			}
		}
		URLClassLoader parentClassLoader = (URLClassLoader) this.getClassLoader().getParent();
		for (URL url : parentClassLoader.getURLs()) {
			String file = url.getFile().split("\\!", 2)[0];
			//TODO: filter out unnecessary jar files
			if (file.endsWith(".jar")) {
				jars.add(file);
			}
		}
		Environment env = this.getApplicationContext().getEnvironment();
		String batchInterval = env.getProperty("batchInterval",
				env.getProperty("spark.streaming.batchInterval", SPARK_STREAMING_BATCH_INTERVAL));
		SparkConf sparkConf = new SparkConf().setMaster(env.getProperty("spark.master.url", SPARK_MASTER_URL))
				.setAppName(getDescriptor().getGroup() + "-" + getDescriptor().getModuleLabel())
				.setJars(jars.toArray(new String[jars.size()]));
		this.streamingContext = new JavaStreamingContext(sparkConf, new Duration(Long.valueOf(batchInterval)));
	}

	@Override
	public void start() {
		super.start();
		//TODO: support multiple receivers with specific partitions
		final Receiver receiver = getComponent(Receiver.class);
		final SparkModule module = getComponent(SparkModule.class);
		final SparkMessageSender sender = (ModuleType.processor.equals(getType()))
				? getComponent(SparkMessageSender.class) : null;
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			@Override
			@SuppressWarnings("unchecked")
			public void run() {
				try {
					JavaDStream input = streamingContext.receiverStream(receiver);
					new SparkModuleExecutor().execute(input, module, sender);
					streamingContext.start();
					streamingContext.awaitTermination();
				}
				catch (Exception e) {
					// ignore
				}
			}
		});
	}

	@Override
	public void stop() {
		try {
			// todo: if possible, change to stop(false, true) without the cancel
			streamingContext.ssc().sc().cancelAllJobs();
			streamingContext.stop();
			super.stop();
		}
		catch (Exception e) {
			// ignore
		}
	}

}
