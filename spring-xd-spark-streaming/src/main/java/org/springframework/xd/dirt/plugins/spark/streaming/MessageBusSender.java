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

package org.springframework.xd.dirt.plugins.spark.streaming;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.interceptor.WireTap;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.xd.dirt.integration.bus.MessageBus;
import org.springframework.xd.spark.streaming.SparkMessageSender;

/**
 * Binds as a producer to the MessageBus in order to send RDD elements from DStreams.
 *
 * @author Ilayaperumal Gopinathan
 * @author Mark Fisher
 * @author Gary Russell
 * @since 1.1
 */
@SuppressWarnings("serial")
class MessageBusSender extends SparkMessageSender {

	/**
	 * Logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(MessageBusSender.class);

	private final String outputChannelName;

	private final LocalMessageBusHolder messageBusHolder;

	private final Properties messageBusProperties;

	private final Properties moduleProducerProperties;

	private final Properties moduleProperties;

	private MessageBus messageBus;

	private ConfigurableApplicationContext applicationContext;

	private boolean running = false;

	private static final String OUTPUT = "output";

	private final MimeType contentType;

	private final SparkStreamingChannel outputChannel;

	private final String tapChannelName;

	private static final String ENABLE_TAP_PROP = "enableTap";

	public MessageBusSender(String outputChannelName, String tapChannelName, Properties messageBusProperties,
			Properties moduleProducerProperties, MimeType contentType, Properties moduleProperties) {
		this(null, outputChannelName, tapChannelName, messageBusProperties, moduleProducerProperties, contentType,
				moduleProperties);
	}

	public MessageBusSender(LocalMessageBusHolder messageBusHolder, String outputChannelName,
			String tapChannelName, Properties messageBusProperties, Properties moduleProducerProperties,
			MimeType contentType, Properties moduleProperties) {
		this.messageBusHolder = messageBusHolder;
		this.outputChannelName = outputChannelName;
		this.tapChannelName = tapChannelName;
		this.messageBusProperties = messageBusProperties;
		this.moduleProducerProperties = moduleProducerProperties;
		this.moduleProperties = moduleProperties;
		this.contentType = contentType;
		this.outputChannel = new SparkStreamingChannel();
	}

	@Override
	public synchronized void start() {
		if (!this.isRunning()) {
			outputChannel.setBeanName(OUTPUT);
			logger.info("starting MessageBusSender");
			if (messageBus == null) {
				if (messageBusHolder != null) {
					messageBus = messageBusHolder.get();
				}
				else {
					applicationContext = MessageBusConfiguration.createApplicationContext(messageBusProperties);
					messageBus = applicationContext.getBean(MessageBus.class);
				}
				if (contentType != null) {
					outputChannel.configureMessageConverter(contentType);
				}
				messageBus.bindProducer(outputChannelName, outputChannel, moduleProducerProperties);
				if (isTapEnabled()) {
					addTapChannel();
				}
			}
			this.running = true;
		}
	}

	/**
	 * Check if the tap is enabled for this module's output.
	 *
	 * @return boolean
	 */
	private boolean isTapEnabled() {
		boolean tapEnabled = false;
		String enableTap = this.moduleProperties.getProperty(ENABLE_TAP_PROP);
		if (enableTap != null) {
			tapEnabled = enableTap.equalsIgnoreCase(Boolean.TRUE.toString());
		}
		return tapEnabled;
	}

	/**
	 * Add tap channel to the module's output channel.
	 * Also, bind the tap channel to the message bus.
	 */
	private void addTapChannel() {
		Assert.notNull(outputChannel, "Output channel can not be null.");
		Assert.notNull(messageBus, "MessageBus can not be null.");
		logger.info("creating and binding tap channel for {}", tapChannelName);
		DirectChannel tapChannel = new DirectChannel();
		tapChannel.setBeanName(tapChannelName + ".tap.bridge");
		messageBus.bindPubSubProducer(tapChannelName, tapChannel, null);
		outputChannel.addInterceptor(new WireTap(tapChannel));
	}

	@Override
	@SuppressWarnings("rawtypes")
	public synchronized void send(Message message) {
		this.outputChannel.send(message);
	}

	@Override
	public synchronized void stop() {
		if (this.isRunning() && messageBus != null) {
			logger.info("stopping MessageBusSender");
			messageBus.unbindProducer(outputChannelName, outputChannel);
			for (ChannelInterceptor interceptor : outputChannel.getChannelInterceptors()) {
				if (interceptor instanceof WireTap) {
					((WireTap) interceptor).stop();
				}
			}
			if (isTapEnabled()) {
				messageBus.unbindProducers(tapChannelName);
			}
			messageBus = null;
		}
		if (applicationContext != null) {
			applicationContext.close();
			applicationContext = null;
		}
		this.running = false;
	}

	@Override
	public synchronized boolean isRunning() {
		return this.running;
	}
}
