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

import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.support.atomic.RedisAtomicLong;
import org.springframework.util.Assert;
import org.springframework.xd.dirt.container.DefaultContainer;
import org.springframework.xd.dirt.core.Container;
import org.springframework.xd.dirt.event.ContainerStartedEvent;
import org.springframework.xd.dirt.server.options.ContainerOptions;
import org.springframework.xd.dirt.server.options.OptionUtils;
import org.springframework.xd.dirt.server.util.BannerUtils;

/**
 * @author Mark Fisher
 * @author Jennifer Hickey
 * @author David Turanski
 */
public class RedisContainerLauncher implements ContainerLauncher, ApplicationEventPublisherAware {

	private final RedisConnectionFactory connectionFactory;

	private volatile RedisAtomicLong ids;

	private volatile ApplicationEventPublisher eventPublisher;

	private static Log logger = LogFactory.getLog(RedisContainerLauncher.class);

	public RedisContainerLauncher(RedisConnectionFactory connectionFactory) {
		Assert.notNull(connectionFactory, "connectionFactory must not be null");
		this.connectionFactory = connectionFactory;
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher eventPublisher) {
		this.eventPublisher = eventPublisher;
	}

	@Override
	public Container launch(ContainerOptions options) {
		synchronized (this) {
			if (this.ids == null) {
				this.ids = new RedisAtomicLong("idsequence", this.connectionFactory);
			}
		}
		long id = ids.incrementAndGet();
		DefaultContainer container = new DefaultContainer(id + "");
		container.start();
		logRedisInfo(container, options);
		container.addListener(new ShutdownListener(container));
		this.eventPublisher.publishEvent(new ContainerStartedEvent(container));
		return container;
	}

	private void logRedisInfo(Container container, ContainerOptions options) {
		if (logger.isInfoEnabled()) {
			final Properties redisInfo = this.connectionFactory.getConnection().info();
			final StringBuilder runtimeInfo = new StringBuilder();
			runtimeInfo.append(String.format("Using Redis v%s (Mode: %s) on port: %s ",
						redisInfo.getProperty("redis_version"),
						redisInfo.getProperty("redis_mode"),
						redisInfo.getProperty("tcp_port")));
			if (options.isJmxDisabled()) {
				runtimeInfo.append(" JMX is disabled for XD components");
			} else {
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
			if (e.getCause() instanceof RedisConnectionFailureException) {
				logger.fatal(e.getCause().getMessage());
				System.err.println("Redis does not seem to be running. Did you install and start Redis? " +
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
