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

import org.springframework.retry.RecoveryCallback;
import org.springframework.retry.RetryContext;

/**
 * A callback that logs information about the failure to execute the redis operation after retries have
 * been exhausted
 *
 * @author Mark Pollack
 */
public class LoggingRecoveryCallback implements RecoveryCallback<Object> {

	private static final Logger logger = LoggerFactory.getLogger(LoggingRecoveryCallback.class);

	@Override
	public Object recover(RetryContext context) throws Exception {
		logger.error("Failed to perform redis operation.", context.getLastThrowable());
		return null;
	}
}
