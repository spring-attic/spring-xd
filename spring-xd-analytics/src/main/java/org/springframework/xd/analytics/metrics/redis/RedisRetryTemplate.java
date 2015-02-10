/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.xd.analytics.metrics.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.retry.RecoveryCallback;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryOperations;

/**
 * Adds Retry functionality to RedisTemplate
 *
 * @author Mark Pollack
 */
public class RedisRetryTemplate<K, V> extends RedisTemplate<K, V> {


	private static final Logger logger = LoggerFactory.getLogger(RedisRetryTemplate.class);

	private volatile RetryOperations retryOperations;

	private volatile RecoveryCallback<?> recoveryCallback = new LoggingRecoveryCallback();

	public RetryOperations RetryOperations() {
		return retryOperations;
	}

	public void setRetryOperations(RetryOperations retryOperations) {
		this.retryOperations = retryOperations;
	}

	public RecoveryCallback<?> getRecoveryCallback() {
		return recoveryCallback;
	}

	public void setRecoveryCallback(RecoveryCallback<?> recoveryCallback) {
		this.recoveryCallback = recoveryCallback;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T execute(final RedisCallback<T> action, final boolean exposeConnection, final boolean pipeline) {
		if (this.retryOperations != null) {
			try {
				return this.retryOperations.execute(new RetryCallback<T, Exception>() {

					@Override
					public T doWithRetry(RetryContext context) throws Exception {
						if (context.getRetryCount() > 0) {
							logger.warn("Retry of Redis Operation. Retry Count = " + context.getRetryCount());
						}
						return RedisRetryTemplate.super.execute(action, exposeConnection, pipeline);
					}

				}, (RecoveryCallback<T>) this.recoveryCallback);
			}
			catch (Exception e) {
				if (e instanceof RuntimeException) {
					throw (RuntimeException) e;
				}
				throw new RedisSystemException("Unknown checked exception translated", e);
			}
		}
		else {
			return super.execute(action, exposeConnection, pipeline);
		}
	}
}
