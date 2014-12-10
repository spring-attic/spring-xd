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
import org.springframework.xd.dirt.integration.bus.MessageBus;
import org.springframework.xd.spark.streaming.SparkMessageSender;

/**
 * Binds as a producer to the MessageBus in order to send RDD elements from DStreams.
 *
 * @author Ilayaperumal Gopinathan
 * @author Mark Fisher
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

	private MessageBus messageBus;

	private ConfigurableApplicationContext applicationContext;

	private boolean running = false;

	public MessageBusSender(String outputChannelName, Properties messageBusProperties, Properties moduleProducerProperties) {
		this(null, outputChannelName, messageBusProperties, moduleProducerProperties);
	}

	public MessageBusSender(LocalMessageBusHolder messageBusHolder, String outputChannelName, Properties messageBusProperties, Properties moduleProducerProperties) {
		this.messageBusHolder = messageBusHolder;
		this.outputChannelName = outputChannelName;
		this.messageBusProperties = messageBusProperties;
		this.moduleProducerProperties = moduleProducerProperties;
	}

	@Override
	public synchronized void start() {
		if (messageBus == null) {
			logger.info("starting MessageBusSender");
			if (messageBusHolder != null) {
				messageBus = messageBusHolder.get();
			}
			else {
				applicationContext = MessageBusConfiguration.createApplicationContext(messageBusProperties);
				messageBus = applicationContext.getBean(MessageBus.class);
			}
			messageBus.bindProducer(outputChannelName, this, moduleProducerProperties);
		}
		this.running = true;
	}

	@Override
	public synchronized void stop() {
		if (this.isRunning() && messageBus != null) {
			logger.info("stopping MessageBusSender");
			messageBus.unbindProducer(outputChannelName, this);
			if (applicationContext != null) {
				applicationContext.close();
				applicationContext = null;
			}
			messageBus = null;
		}
		this.running = false;
	}

	@Override
	public synchronized boolean isRunning() {
		return this.running;
	}

}
