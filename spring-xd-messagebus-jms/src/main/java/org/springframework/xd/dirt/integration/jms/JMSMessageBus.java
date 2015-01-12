/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.xd.dirt.integration.jms;

import java.util.Properties;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.Topic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.jms.ChannelPublishingJmsMessageListener;
import org.springframework.integration.jms.JmsMessageDrivenEndpoint;
import org.springframework.integration.jms.JmsSendingMessageHandler;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.SessionCallback;
import org.springframework.jms.listener.SimpleMessageListenerContainer;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.util.Assert;
import org.springframework.xd.dirt.integration.bus.AbstractBusPropertiesAccessor;
import org.springframework.xd.dirt.integration.bus.Binding;
import org.springframework.xd.dirt.integration.bus.MessageBusSupport;
import org.springframework.xd.dirt.integration.bus.serializer.MultiTypeCodec;


public class JMSMessageBus extends MessageBusSupport implements DisposableBean {

	private static final String DEFAULT_JMS_PREFIX = "xdbus.";

	private static Logger logger = LoggerFactory.getLogger(JMSMessageBus.class);

	private final ConnectionFactory connectionFactory;

	private final JmsTemplate jmsTemplate;


	public JMSMessageBus(ConnectionFactory connectionFactory, MultiTypeCodec<Object> codec) {
		Assert.notNull(connectionFactory, "connectionFactory must not be null");
		Assert.notNull(codec, "codec must not be null");
		this.connectionFactory = connectionFactory;
		this.setCodec(codec);
		this.jmsTemplate = new JmsTemplate(connectionFactory);
		this.jmsTemplate.afterPropertiesSet();

	}

	@Override
	public void bindConsumer(final String name, MessageChannel moduleInputChannel, final Properties properties) {
		logger.info("declaring queue for inbound: {} ", name);
		Queue queue = jmsTemplate.execute(new SessionCallback<Queue>() {

			@Override
			public Queue doInJms(Session session) throws JMSException {
				return session.createQueue(DEFAULT_JMS_PREFIX + name);
			}
		});

		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
		container.setConnectionFactory(connectionFactory);
		container.setDestination(queue);
		container.afterPropertiesSet();
		ChannelPublishingJmsMessageListener channelPublishingJmsMessageListener = new ChannelPublishingJmsMessageListener();
		channelPublishingJmsMessageListener.setExpectReply(false);
		channelPublishingJmsMessageListener.setRequestChannel(moduleInputChannel);
		channelPublishingJmsMessageListener.setBeanFactory(this.getBeanFactory());
		channelPublishingJmsMessageListener.afterPropertiesSet();
		JmsMessageDrivenEndpoint endpoint = new JmsMessageDrivenEndpoint(container, channelPublishingJmsMessageListener);
		endpoint.setBeanFactory(getBeanFactory());
		endpoint.setBeanName("inbound." + name);
		endpoint.afterPropertiesSet();

		Binding consumerBinding = Binding.forConsumer(name, endpoint, moduleInputChannel,
				new AbstractBusPropertiesAccessor(properties) {

		});
		addBinding(consumerBinding);
		consumerBinding.start();
	}

	@Override
	public void bindPubSubConsumer(final String name, MessageChannel moduleInputChannel, Properties properties) {
		logger.info("declaring topic for inbound: {} ", name);
		Topic topic = jmsTemplate.execute(new SessionCallback<Topic>() {

			@Override
			public Topic doInJms(Session session) throws JMSException {
				return session.createTopic(DEFAULT_JMS_PREFIX + "topic" + name);
			}
		});

		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
		container.setConnectionFactory(connectionFactory);
		container.setDestination(topic);
		container.afterPropertiesSet();
		ChannelPublishingJmsMessageListener channelPublishingJmsMessageListener = new ChannelPublishingJmsMessageListener();
		channelPublishingJmsMessageListener.setExpectReply(false);
		channelPublishingJmsMessageListener.setRequestChannel(moduleInputChannel);
		channelPublishingJmsMessageListener.setBeanFactory(this.getBeanFactory());
		channelPublishingJmsMessageListener.afterPropertiesSet();
		JmsMessageDrivenEndpoint endpoint = new JmsMessageDrivenEndpoint(container, channelPublishingJmsMessageListener);
		endpoint.setBeanFactory(getBeanFactory());
		endpoint.setBeanName("inbound." + name);
		endpoint.afterPropertiesSet();

		Binding consumerBinding = Binding.forConsumer(name, endpoint, moduleInputChannel,
				new AbstractBusPropertiesAccessor(properties) {

		});
		addBinding(consumerBinding);
		consumerBinding.start();
	}


	@Override
	public void bindProducer(final String name, MessageChannel moduleOutputChannel, final Properties properties) {
		Assert.isInstanceOf(SubscribableChannel.class, moduleOutputChannel);
		logger.info("declaring queue for outbound: {} ", name);

		Queue queue = jmsTemplate.execute(new SessionCallback<Queue>() {

			@Override
			public Queue doInJms(Session session) throws JMSException {
				return session.createQueue(DEFAULT_JMS_PREFIX + name);
			}
		});

		JmsSendingMessageHandler handler = new JmsSendingMessageHandler(jmsTemplate);
		handler.setBeanFactory(this.getBeanFactory());
		handler.setDestination(queue);
		handler.afterPropertiesSet();

		EventDrivenConsumer consumer = new EventDrivenConsumer((SubscribableChannel) moduleOutputChannel, handler);
		consumer.setBeanFactory(getBeanFactory());
		consumer.setComponentName(name);
		consumer.setBeanName("outbound." + name);
		consumer.afterPropertiesSet();


		Binding producerBinding = Binding.forProducer(name, moduleOutputChannel, consumer,
				new AbstractBusPropertiesAccessor(properties) {

		});
		addBinding(producerBinding);
		producerBinding.start();
	}

	@Override
	public void bindPubSubProducer(final String name, MessageChannel moduleOutputChannel, Properties properties) {
		logger.info("declaring topic for outbound: {} ", name);
		Assert.isInstanceOf(SubscribableChannel.class, moduleOutputChannel);

		Topic topic = jmsTemplate.execute(new SessionCallback<Topic>() {

			@Override
			public Topic doInJms(Session session) throws JMSException {
				return session.createTopic(DEFAULT_JMS_PREFIX + "topic" + name);
			}
		});

		JmsSendingMessageHandler handler = new JmsSendingMessageHandler(jmsTemplate);
		handler.setBeanFactory(this.getBeanFactory());
		handler.setDestination(topic);
		handler.afterPropertiesSet();

		EventDrivenConsumer consumer = new EventDrivenConsumer((SubscribableChannel) moduleOutputChannel, handler);
		consumer.setBeanFactory(getBeanFactory());
		consumer.setBeanName("outbound." + name);
		consumer.afterPropertiesSet();


		Binding producerBinding = Binding.forProducer(name, moduleOutputChannel, consumer,
				new AbstractBusPropertiesAccessor(properties) {

		});
		addBinding(producerBinding);
		producerBinding.start();
	}

	@Override
	public void bindRequestor(String name, MessageChannel requests, MessageChannel replies, Properties properties) {
		throw new UnsupportedOperationException("bindRequestor() is not implemented.");
	}

	@Override
	public void bindReplier(String name, MessageChannel requests, MessageChannel replies, Properties properties) {
		throw new UnsupportedOperationException("bindReplier() is not implemented.");
	}

	@Override
	public void destroy() throws Exception {
		stopBindings();
	}


}
