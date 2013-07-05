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

package org.springframework.xd.dirt.launcher;

import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.util.Assert;
import org.springframework.xd.dirt.container.DefaultContainer;
import org.springframework.xd.dirt.core.Container;
import org.springframework.xd.dirt.event.ContainerStartedEvent;
import org.springframework.xd.dirt.server.options.ContainerOptions;
import org.springframework.xd.dirt.server.options.OptionUtils;
import org.springframework.xd.dirt.server.util.BannerUtils;

/**
 * @author Mark Fisher
 */
public class RabbitContainerLauncher implements ContainerLauncher, ApplicationEventPublisherAware {

	private final ConnectionFactory connectionFactory;

	private volatile ApplicationEventPublisher eventPublisher;

	private static Log logger = LogFactory.getLog(RabbitContainerLauncher.class);

	public RabbitContainerLauncher(ConnectionFactory connectionFactory) {
		Assert.notNull(connectionFactory, "connectionFactory must not be null");
		this.connectionFactory = connectionFactory;
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher eventPublisher) {
		this.eventPublisher = eventPublisher;
	}

	@Override
	public Container launch(ContainerOptions options) {
		String id = UUID.randomUUID().toString();
		DefaultContainer container = new DefaultContainer(id);
		container.start();
		logRabbitInfo(container, options);
		container.addListener(new ShutdownListener(container));
		this.eventPublisher.publishEvent(new ContainerStartedEvent(container));
		return container;
	}

	private void logRabbitInfo(Container container, ContainerOptions options) {
		if (logger.isInfoEnabled()) {
			final StringBuilder runtimeInfo = new StringBuilder();
			runtimeInfo.append(String.format("Using RabbitMQ at %s (virtual host: %s) on port: %d ",
						this.connectionFactory.getHost(),
						this.connectionFactory.getVirtualHost(),
						this.connectionFactory.getPort()));
			if (options.isJmxDisabled()) {
				runtimeInfo.append(" JMX is disabled for XD components");
			}
			else {
				runtimeInfo.append(String.format(" JMX port: %d", options.getJmxPort()));
			}
			logger.info(BannerUtils.displayBanner(container.getJvmName(), runtimeInfo.toString()));
		}
	}

	/**
	 * Create a container instance
	 * @param options
	 */
	@SuppressWarnings("resource")
	public static Container create(ContainerOptions options) {
		ClassPathXmlApplicationContext context = null;
		try {
			context = new ClassPathXmlApplicationContext();
			context.setConfigLocation(LAUNCHER_CONFIG_LOCATION);
			//TODO: Need to sort out how this will be handled consistently among launchers
			if (!options.isJmxDisabled()) {
				context.getEnvironment().addActiveProfile("xd.jmx.enabled");
				OptionUtils.setJmxProperties(options, context.getEnvironment());
			}
			context.refresh();
		}
		catch (BeanCreationException e) {
			if (e.getCause() instanceof AmqpConnectException) {
				logger.fatal(e.getCause().getMessage());
				System.err.println("RabbitMQ does not seem to be running. Did you install and start RabbitMQ? " +
						"Please see the Getting Started section of the guide for instructions.");
				System.exit(1);
			}
		}
		context.registerShutdownHook();
		ContainerLauncher launcher = context.getBean(ContainerLauncher.class);
		Container container = launcher.launch(options);
		return container;
	}


	private static class ShutdownListener implements ApplicationListener<ContextClosedEvent> {

		private final Container container;

		ShutdownListener(Container container) {
			this.container = container;
		}

		@Override
		public void onApplicationEvent(ContextClosedEvent event) {
			container.stop();
		}
	}
}
