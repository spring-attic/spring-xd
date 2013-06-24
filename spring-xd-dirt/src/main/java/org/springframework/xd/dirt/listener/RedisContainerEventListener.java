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

package org.springframework.xd.dirt.listener;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.xd.dirt.core.Container;
import org.springframework.xd.dirt.event.ContainerStartedEvent;
import org.springframework.xd.dirt.event.ContainerStoppedEvent;

/**
 * @author Mark Fisher
 * @author Jennifer Hickey
 * @author Gary Russell
 * @author Gunnar Hillert
 * @author David Turanski
 */
public class RedisContainerEventListener extends AbstractContainerEventListener {

	private final StringRedisTemplate redisTemplate = new StringRedisTemplate();

	public RedisContainerEventListener(RedisConnectionFactory redisConnectionFactory) {
		this.redisTemplate.setConnectionFactory(redisConnectionFactory);
		this.redisTemplate.afterPropertiesSet();
	}

	@Override
	protected void onContainerStartedEvent(ContainerStartedEvent event) {
		final Container container = event.getSource();
		this.redisTemplate.boundHashOps("containers").put(container.getId(), container.getJvmName());
	}

	@Override
	protected void onContainerStoppedEvent(ContainerStoppedEvent event) {
		Container container = event.getSource();
		this.redisTemplate.boundHashOps("containers").delete(container.getId());
		this.redisTemplate.delete(container.getId());
	}

}
