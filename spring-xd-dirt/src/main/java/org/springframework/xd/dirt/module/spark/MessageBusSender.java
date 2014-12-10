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

package org.springframework.xd.dirt.module.spark;

import java.util.Properties;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.xd.dirt.integration.bus.MessageBus;
import org.springframework.xd.module.spark.SparkMessageSender;

/**
 * Binds as a producer to the MessageBus in order to send RDD elements from DStreams.
 *
 * @author Ilayaperumal Gopinathan
 * @author Mark Fisher
 */
public class MessageBusSender extends SparkMessageSender {

	private final String outputChannelName;

	private final Properties properties;

	private MessageBus messageBus;

	private ConfigurableApplicationContext applicationContext;

	public MessageBusSender(String outputChannelName, Properties properties) {
		this.outputChannelName = outputChannelName;
		this.properties = properties;
	}

	public void start() {
		if (applicationContext != null) {
			return;
		}
		applicationContext = MessageBusConfiguration.createApplicationContext(properties);
		messageBus = applicationContext.getBean(MessageBus.class);
		messageBus.bindProducer(outputChannelName, this, null);
	}

	public void stop() {
		if (messageBus != null) {
			messageBus.unbindProducer(outputChannelName, this);
			applicationContext.close();
			applicationContext = null;
		}
	}

}
