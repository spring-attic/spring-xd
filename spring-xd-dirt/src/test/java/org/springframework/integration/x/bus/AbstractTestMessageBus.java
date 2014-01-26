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

package org.springframework.integration.x.bus;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.springframework.http.MediaType;
import org.springframework.messaging.MessageChannel;


/**
 * Abstract class that adds test support for {@link MessageBus}.
 * 
 * @author Ilayaperumal Gopinathan
 */
public abstract class AbstractTestMessageBus implements MessageBus {

	protected Set<String> queues = new HashSet<String>();

	protected Set<String> topics = new HashSet<String>();

	private final MessageBus messageBus;

	public AbstractTestMessageBus(MessageBus messageBus) {
		this.messageBus = messageBus;
	}

	@Override
	public void bindConsumer(String name, MessageChannel moduleInputChannel, Collection<MediaType> acceptedMediaTypes,
			boolean aliasHint) {
		messageBus.bindConsumer(name, moduleInputChannel, acceptedMediaTypes, aliasHint);
		queues.add(name);
	}

	@Override
	public void bindPubSubConsumer(String name, MessageChannel inputChannel, Collection<MediaType> acceptedMediaTypes) {
		messageBus.bindPubSubConsumer(name, inputChannel, acceptedMediaTypes);
		addTopic(name);
	}

	@Override
	public void bindProducer(String name, MessageChannel moduleOutputChannel, boolean aliasHint) {
		messageBus.bindProducer(name, moduleOutputChannel, aliasHint);
		queues.add(name);
	}

	@Override
	public void bindPubSubProducer(String name, MessageChannel outputChannel) {
		messageBus.bindPubSubProducer(name, outputChannel);
		addTopic(name);
	}

	@Override
	public void bindRequestor(String name, MessageChannel requests, MessageChannel replies) {
		messageBus.bindRequestor(name, requests, replies);
		queues.add(name + ".requests");
	}

	@Override
	public void bindReplier(String name, MessageChannel requests, MessageChannel replies) {
		messageBus.bindReplier(name, requests, replies);
		queues.add(name + ".requests");
	}

	private void addTopic(String topicName) {
		topics.add("topic." + topicName);
	}

	public MessageBus getCoreMessageBus() {
		return messageBus;
	}

	public abstract void cleanup();

	@Override
	public void unbindConsumers(String name) {
		messageBus.unbindConsumers(name);
	}

	@Override
	public void unbindProducers(String name) {
		messageBus.unbindProducers(name);
	}

	@Override
	public void unbindConsumer(String name, MessageChannel channel) {
		messageBus.unbindConsumer(name, channel);
	}

	@Override
	public void unbindProducer(String name, MessageChannel channel) {
		messageBus.unbindProducer(name, channel);
	}

}
