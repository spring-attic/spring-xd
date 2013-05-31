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

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.support.atomic.RedisAtomicLong;
import org.springframework.util.StringUtils;
import org.springframework.xd.dirt.container.DefaultContainer;
import org.springframework.xd.dirt.core.Container;
import org.springframework.xd.dirt.event.ContainerStartedEvent;

/**
 * @author Mark Fisher
 * @author Jennifer Hickey
 */
public class RedisContainerLauncher implements ContainerLauncher, ApplicationEventPublisherAware {

	private final RedisAtomicLong ids;

	private volatile ApplicationEventPublisher eventPublisher;


	public RedisContainerLauncher(RedisConnectionFactory connectionFactory) {
		this.ids = new RedisAtomicLong("idsequence", connectionFactory);
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher eventPublisher) {
		this.eventPublisher = eventPublisher;
	}

	@Override
	public Container launch() {
		long id = ids.incrementAndGet();
		DefaultContainer container = new DefaultContainer(id + "");
		container.start();
		container.addListener(new ShutdownListener(container));
		this.eventPublisher.publishEvent(new ContainerStartedEvent(container));
		return container;
	}

	public static void main(String[] args) {		
		if (!StringUtils.hasText(System.getProperty("xd.home"))) {
			String xdhome = (args.length > 0) ? args[0] : "..";
			System.setProperty("xd.home", xdhome);
		}
		if (args.length > 1) {
			setRedisProperties(args[1], args[2]);
		}
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("META-INF/spring/launcher.xml");
		context.registerShutdownHook();
		ContainerLauncher launcher = context.getBean(ContainerLauncher.class);
		launcher.launch();
	}
	
	/**
	 * Set system properties to override PropertyPlaceholderConfigurer 
	 * properties for the Container 
	 * @param redisHost
	 * @param redisPort
	 */
	private static void setRedisProperties(String redisHost, String redisPort) {
		System.setProperty("redis.hostname", redisHost);
		System.setProperty("redis.port", String.valueOf(redisPort));
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
