/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.integration.x.rabbit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.Lifecycle;
import org.springframework.http.MediaType;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.amqp.inbound.AmqpInboundChannelAdapter;
import org.springframework.integration.amqp.outbound.AmqpOutboundEndpoint;
import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.core.SubscribableChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.x.channel.registry.ChannelRegistry;
import org.springframework.integration.x.channel.registry.ChannelRegistrySupport;
import org.springframework.util.Assert;
import org.springframework.xd.module.Module;

/**
 * A {@link ChannelRegistry} implementation backed by RabbitMQ.
 *
 * @author Mark Fisher
 * @author Gary Russell
 */
public class RabbitChannelRegistry extends ChannelRegistrySupport implements DisposableBean {

	private final Log logger = LogFactory.getLog(this.getClass());

	private final RabbitAdmin rabbitAdmin;

	private final RabbitTemplate rabbitTemplate = new RabbitTemplate();

	private final ConnectionFactory connectionFactory;

	private volatile Integer concurrentConsumers;

	private final List<Lifecycle> lifecycleBeans = Collections.synchronizedList(new ArrayList<Lifecycle>());


	public RabbitChannelRegistry(ConnectionFactory connectionFactory) {
		Assert.notNull(connectionFactory, "connectionFactory must not be null");
		this.connectionFactory = connectionFactory;
		this.rabbitTemplate.setConnectionFactory(connectionFactory);
		this.rabbitTemplate.afterPropertiesSet();
		this.rabbitAdmin = new RabbitAdmin(connectionFactory);
		this.rabbitAdmin.afterPropertiesSet();
	}

	@Override
	public void inbound(final String name, MessageChannel channel, Module module) {
		if (logger.isInfoEnabled()) {
			logger.info("declaring queue for inbound: " + name);
		}
		this.rabbitAdmin.declareQueue(new Queue(name));
		SimpleMessageListenerContainer listenerContainer = new SimpleMessageListenerContainer(this.connectionFactory);
		if (this.concurrentConsumers != null) {
			listenerContainer.setConcurrentConsumers(this.concurrentConsumers);
		}
		listenerContainer.setQueueNames(name);
		listenerContainer.afterPropertiesSet();
		AmqpInboundChannelAdapter adapter = new AmqpInboundChannelAdapter(listenerContainer);
		adapter.setOutputChannel(channel);
		adapter.setBeanName("inbound." + name);
		adapter.afterPropertiesSet();
		this.lifecycleBeans.add(adapter);
		adapter.start();
		//TODO: add conversion, similar to Redis
	}

	@Override
	public void outbound(final String name, MessageChannel channel, Module module) {
		Assert.isInstanceOf(SubscribableChannel.class, channel);
		MessageHandler handler = new CompositeHandler(name);
		EventDrivenConsumer consumer = new EventDrivenConsumer((SubscribableChannel) channel, handler);
		consumer.setBeanName("outbound." + name);
		consumer.afterPropertiesSet();
		this.lifecycleBeans.add(consumer);
		consumer.start();
	}

	@Override
	public void tap(String tapModule, final String name, MessageChannel channel) {
		SimpleMessageListenerContainer listenerContainer = new SimpleMessageListenerContainer(this.connectionFactory);
		if (this.concurrentConsumers != null) {
			listenerContainer.setConcurrentConsumers(this.concurrentConsumers);
		}
		Queue queue = this.rabbitAdmin.declareQueue();
		Binding binding = BindingBuilder.bind(queue).to(new FanoutExchange("tap." + name));
		this.rabbitAdmin.declareBinding(binding);
		listenerContainer.setQueues(queue);
		listenerContainer.afterPropertiesSet();
		AmqpInboundChannelAdapter adapter = new AmqpInboundChannelAdapter(listenerContainer);
		adapter.setOutputChannel(channel);
		adapter.setBeanName("tap." + name);
		adapter.setComponentName(tapModule + "." + "tapAdapter");
		adapter.afterPropertiesSet();
		this.lifecycleBeans.add(adapter);
		adapter.start();
	}

	@Override
	public void cleanAll(String name) {
		synchronized (this.lifecycleBeans) {
			Iterator<Lifecycle> iterator = this.lifecycleBeans.iterator();
			while (iterator.hasNext()) {
				Lifecycle endpoint = iterator.next();
				if (endpoint instanceof EventDrivenConsumer
						&& ("outbound." + name).equals(((IntegrationObjectSupport) endpoint).getComponentName())) {
					((EventDrivenConsumer) endpoint).stop();
					iterator.remove();
				}
				else if (endpoint instanceof AmqpInboundChannelAdapter) {
					String componentName = ((IntegrationObjectSupport) endpoint).getComponentName();
					if (("inbound." + name).equals(componentName) ||
							(name + ".tapAdapter").equals(componentName)) {
						((AmqpInboundChannelAdapter) endpoint).stop();
						iterator.remove();
					}
				}
			}
		}
	}

	@Override
	public void destroy() {
		for (Lifecycle bean : this.lifecycleBeans) {
			try {
				bean.stop();
			}
			catch (Exception e) {
				if (logger.isWarnEnabled()) {
					logger.warn("failed to stop adapter", e);
				}
			}
		}
	}

	private class CompositeHandler extends AbstractMessageHandler {

		private final AmqpOutboundEndpoint queue;

		private final AmqpOutboundEndpoint tap;

		private CompositeHandler(String name) {
			if (logger.isInfoEnabled()) {
				logger.info("declaring queue for outbound: " + name);
			}
			rabbitAdmin.declareQueue(new Queue(name));
			rabbitAdmin.declareExchange(new FanoutExchange("tap." + name));
			this.queue = new AmqpOutboundEndpoint(rabbitTemplate);
			queue.setRoutingKey(name); // uses default exchange
			queue.afterPropertiesSet();
			this.tap = new AmqpOutboundEndpoint(rabbitTemplate);
			tap.setExchangeName("tap." + name);
			tap.afterPropertiesSet();
		}

		@Override
		protected void handleMessageInternal(Message<?> message) throws Exception {
			// TODO: rabbit wire data pluggable format?
			Message<?> messageToSend = transformOutboundIfNecessary(message, MediaType.APPLICATION_JSON);
			this.tap.handleMessage(messageToSend);
			this.queue.handleMessage(messageToSend);
		}
	}

}
