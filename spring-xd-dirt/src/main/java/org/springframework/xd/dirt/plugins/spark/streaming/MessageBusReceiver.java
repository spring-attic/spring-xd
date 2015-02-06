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

import org.apache.spark.storage.StorageLevel;
import org.apache.spark.streaming.receiver.Receiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.Message;
import org.springframework.util.MimeType;
import org.springframework.xd.dirt.integration.bus.MessageBus;

/**
 * Spark {@link Receiver} implementation that binds to the MessageBus as a consumer.
 *
 * @author Mark Fisher
 * @author Ilayaperumal Gopinathan
 */
@SuppressWarnings({"rawtypes", "unchecked"})
class MessageBusReceiver extends Receiver {

	private static final long serialVersionUID = 1L;

	/**
	 * Logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(MessageBusReceiver.class);

	private MessageBus messageBus;

	private ConfigurableApplicationContext applicationContext;

	private String channelName;

	private final LocalMessageBusHolder messageBusHolder;

	private final Properties messageBusProperties;

	private final Properties moduleConsumerProperties;

	private final MimeType contentType;

	public MessageBusReceiver(StorageLevel storageLevel, Properties messageBusProperties,
			Properties moduleConsumerProperties, MimeType contentType) {
		this(null, storageLevel, messageBusProperties, moduleConsumerProperties, contentType);
	}

	public MessageBusReceiver(LocalMessageBusHolder messageBusHolder, StorageLevel storageLevel,
			Properties messageBusProperties, Properties moduleConsumerProperties, MimeType contentType) {
		super(storageLevel);
		this.messageBusHolder = messageBusHolder;
		this.messageBusProperties = messageBusProperties;
		this.moduleConsumerProperties = moduleConsumerProperties;
		this.contentType = contentType;
	}

	public void setInputChannelName(String channelName) {
		this.channelName = channelName;
	}

	@Override
	public void onStart() {
		logger.info("starting MessageBusReceiver");
		final MessageStoringChannel messageStoringChannel = new MessageStoringChannel();
		if (contentType != null) {
			messageStoringChannel.configureMessageConverter(contentType);
		}
		MessageBus messageBus;
		if (messageBusHolder != null) {
			messageBus = messageBusHolder.get();
		}
		else {
			applicationContext = MessageBusConfiguration.createApplicationContext(messageBusProperties);
			messageBus = applicationContext.getBean(MessageBus.class);
		}
		messageBus.bindConsumer(channelName, messageStoringChannel, moduleConsumerProperties);
	}

	@Override
	public void onStop() {
		logger.info("stopping MessageBusReceiver");
		messageBus.unbindConsumers(channelName);
		if (applicationContext != null) {
			applicationContext.close();
		}
	}

	/**
	 * The {@link DirectChannel} that stores the received messages into Spark's memory.
	 */
	private class MessageStoringChannel extends SparkStreamingChannel {

		private static final long serialVersionUID = 1L;

		private static final String INPUT = "input";

		public MessageStoringChannel() {
			this.setBeanName(INPUT);
		}

		@Override
		protected boolean doSend(Message<?> message, long timeout) {
			store(message.getPayload());
			return true;
		}
	}

}
