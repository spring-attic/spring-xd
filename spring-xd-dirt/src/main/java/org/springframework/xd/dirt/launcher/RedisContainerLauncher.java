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

import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.support.atomic.RedisAtomicLong;
import org.springframework.util.Assert;
import org.springframework.xd.dirt.container.XDContainer;
import org.springframework.xd.dirt.server.util.BannerUtils;

/**
 * @author Mark Fisher
 * @author Jennifer Hickey
 * @author David Turanski
 */
public class RedisContainerLauncher extends AbstractContainerLauncher {

	private final RedisConnectionFactory connectionFactory;

	private volatile RedisAtomicLong ids;


	public RedisContainerLauncher(RedisConnectionFactory connectionFactory) {
		Assert.notNull(connectionFactory, "connectionFactory must not be null");
		this.connectionFactory = connectionFactory;
	}


	@Override
	protected String generateId() {
		synchronized (this) {
			if (this.ids == null) {
				this.ids = new RedisAtomicLong("idsequence", this.connectionFactory);
			}
		}
		return ids.incrementAndGet() + "";
	}

	@Override
	protected void logContainerInfo(Log logger, XDContainer container) {
		if (logger.isInfoEnabled()) {
			final Properties redisInfo = this.connectionFactory.getConnection().info();
			final StringBuilder runtimeInfo = new StringBuilder();
			runtimeInfo.append(String.format("Using Redis v%s (Mode: %s) on port: %s ",
					redisInfo.getProperty("redis_version"),
					redisInfo.getProperty("redis_mode"),
					redisInfo.getProperty("tcp_port")));
			if (container.isJmxEnabled()) {
				runtimeInfo.append(String.format(" JMX port: %d", container.getJmxPort()));
			}
			else {
				runtimeInfo.append(" JMX is disabled for XD components");
			}
			logger.info(BannerUtils.displayBanner(container.getJvmName(), runtimeInfo.toString()));
		}
	}

	@Override
	protected void logErrorInfo(Exception exception) {
		if (exception instanceof RedisConnectionFailureException) {
			System.err.println("Redis does not seem to be running. " +
					"Did you install and start Redis?" +
					"Please see the Getting Started section of the guide for instructions.");
		}
	}
}
