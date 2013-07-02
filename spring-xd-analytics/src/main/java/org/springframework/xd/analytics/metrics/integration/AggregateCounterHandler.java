/*
 * Copyright 2011-2013 the original author or authors.
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

package org.springframework.xd.analytics.metrics.integration;

import org.springframework.integration.Message;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.util.Assert;
import org.springframework.xd.analytics.metrics.core.AggregateCounterService;

/**
 * @author Luke Taylor
 */
public class AggregateCounterHandler {
	private final AggregateCounterService counterService;
	private final String counterName;

	public AggregateCounterHandler(AggregateCounterService counterService, String counterName) {
		Assert.notNull(counterService, "Counter Service can not be null");
		Assert.notNull(counterName, "Counter Name can not be null");
		this.counterService = counterService;
		this.counterName = counterName;
	}

	@ServiceActivator
	public Message<?> process(Message<?> message) {
		if (message != null) {
			this.counterService.increment(counterName);
		}
		return message;
	}
}
