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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.support.atomic.RedisAtomicLong;
import org.springframework.xd.dirt.container.DefaultContainer;
import org.springframework.xd.dirt.core.Container;
import org.springframework.xd.dirt.event.ContainerStartedEvent;

/**
 * @author Mark Fisher
 */
public class RedisContainerLauncher implements ContainerLauncher, ApplicationEventPublisherAware {

	private final RedisAtomicLong ids;

	private volatile ApplicationEventPublisher eventPublisher;

	private final ShutdownListener shutdownListener;

	public RedisContainerLauncher(RedisConnectionFactory connectionFactory) {
		this.ids = new RedisAtomicLong("idsequence", connectionFactory);
		this.shutdownListener = new ShutdownListener(connectionFactory);
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
		container.addListener(this.shutdownListener);
		this.eventPublisher.publishEvent(new ContainerStartedEvent(container));
		return container;
	}

	public static void main(String[] args) {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("META-INF/spring/launcher.xml");
		context.registerShutdownHook();
		ContainerLauncher launcher = context.getBean(ContainerLauncher.class);
		launcher.launch();
	}

	private static class ShutdownListener implements ApplicationListener<ContextClosedEvent> {

		private final StringRedisTemplate redisTemplate = new StringRedisTemplate();

		ShutdownListener(RedisConnectionFactory connectionFactory) {
			this.redisTemplate.setConnectionFactory(connectionFactory);
			this.redisTemplate.afterPropertiesSet();
		}

		@Override
		public void onApplicationEvent(ContextClosedEvent event) {
			String id = event.getApplicationContext().getId();
			this.redisTemplate.boundHashOps("containers").delete(id);
			this.redisTemplate.delete(id);
		}
	}

}
