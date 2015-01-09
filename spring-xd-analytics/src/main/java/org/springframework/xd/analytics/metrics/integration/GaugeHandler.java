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

import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;
import org.springframework.xd.analytics.metrics.core.GaugeRepository;

/**
 * @author David Turanski
 * @author Luke Taylor
 *
 */
public class GaugeHandler extends AbstractMetricHandler {

	private final GaugeRepository gaugeRepository;

	public GaugeHandler(GaugeRepository gaugeRepository, String nameExpression) {
		super(nameExpression);
		Assert.notNull(gaugeRepository, "Gauge Repository can not be null");
		this.gaugeRepository = gaugeRepository;
	}

	@ServiceActivator
	public void process(Message<?> message) {
		if (message != null) {
			long value = convertToLong(message.getPayload());
			this.gaugeRepository.recordValue(computeMetricName(message), value);
		}
	}

	/**
	 * @param payload
	 * @return long value
	 */
	long convertToLong(Object payload) {
		if (payload != null) {
			if (payload instanceof Number) {
				return ((Number) payload).longValue();
			}
			else if (payload instanceof String) {
				try {
					return Long.parseLong((String) payload);
				}
				catch (Exception e) {
					throw new MessagingException("cannot convert payload to long", e);
				}
			}
		}
		throw new MessagingException("cannot convert "
				+ (payload == null ? "null" : payload.getClass().getName() + " to long"));
	}

}
