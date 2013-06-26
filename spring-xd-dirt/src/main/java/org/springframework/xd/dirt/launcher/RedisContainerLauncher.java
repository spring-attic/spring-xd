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
import org.springframework.xd.dirt.listener.util.BannerUtils;

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
	public Container launch() {
		synchronized (this) {
			if (this.ids == null) {
				this.ids = new RedisAtomicLong("idsequence", this.connectionFactory);
			}
		}
		long id = ids.incrementAndGet();
		DefaultContainer container = new DefaultContainer(id + "");
		container.start();
		logRedisInfo(container);
		container.addListener(new ShutdownListener(container));
		this.eventPublisher.publishEvent(new ContainerStartedEvent(container));
		return container;
	}

	private void logRedisInfo(Container container) {
		if (logger.isInfoEnabled()) {
			final Properties redisInfo = this.connectionFactory.getConnection().info();
			final StringBuilder sb = new StringBuilder();
			sb.append(String.format("Using Redis v%s (Mode: %s) on Port %s ",
						redisInfo.getProperty("redis_version"),
						redisInfo.getProperty("redis_mode"),
						redisInfo.getProperty("tcp_port")));
			logger.info(BannerUtils.displayBanner(container.getJvmName(), sb.toString()));
		}
	}

	public static void main(String... args) {
		ClassPathXmlApplicationContext context = null;
		try {
			context = new ClassPathXmlApplicationContext();
			context.setConfigLocation(LAUNCHER_CONFIG_LOCATION);
			//TODO: Need to sort out how this will be handled consistently among launchers
			if (System.getProperty("xd.jmx.disabled") == null) {
				context.getEnvironment().addActiveProfile("xd.jmx.enabled");
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
		launcher.launch();
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
