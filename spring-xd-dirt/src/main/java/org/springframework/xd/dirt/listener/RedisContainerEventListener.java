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

import java.lang.management.ManagementFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.xd.dirt.core.Container;
import org.springframework.xd.dirt.event.ContainerStartedEvent;

/**
 * @author Mark Fisher
 */
public class RedisContainerEventListener extends AbstractContainerEventListener {

	private final Log logger = LogFactory.getLog(this.getClass());

	private final StringRedisTemplate redisTemplate = new StringRedisTemplate();

	public RedisContainerEventListener(RedisConnectionFactory redisConnectionFactory) {
		this.redisTemplate.setConnectionFactory(redisConnectionFactory);
		this.redisTemplate.afterPropertiesSet();
	}

	@Override
	protected void onContainerStartedEvent(ContainerStartedEvent event) {
		String name = ManagementFactory.getRuntimeMXBean().getName();
		if (logger.isInfoEnabled()) {
			logger.info("started container: " + name);
		}
		Container container = event.getSource();
		this.redisTemplate.boundHashOps("containers").put(container.getId(), name);
	}

}
