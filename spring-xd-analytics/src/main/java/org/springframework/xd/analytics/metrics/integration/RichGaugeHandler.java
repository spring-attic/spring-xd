/*
 * Copyright 2002-2013 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.xd.analytics.metrics.integration;

import org.springframework.integration.Message;
import org.springframework.integration.MessagingException;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.util.Assert;
import org.springframework.xd.analytics.metrics.core.RichGaugeService;

/**
 * @author David Turanski
 *
 */
public class RichGaugeHandler {
	private final RichGaugeService richGaugeService;
	private final String name;

	public RichGaugeHandler(RichGaugeService richGaugeService, String name) {
		Assert.notNull(richGaugeService, "Rich Gauge Service can not be null");
		Assert.notNull(name, "Rich Gauge Name can not be null");
		this.richGaugeService = richGaugeService;
		this.name = name;
		this.richGaugeService.getOrCreate(name);
	}

	@ServiceActivator
	public void process(Message<?> message) {
		if (message != null) {
			double value = convertToDouble(message.getPayload());
			this.richGaugeService.setValue(name, value);
		}
	}

	/**
	 * @param payload
	 * @return double value
	 */
	double convertToDouble(Object payload) {
		if (payload != null) {
			if (payload instanceof Integer) {
				return ((Integer) payload).doubleValue();
			} else if (payload instanceof Double) {
				return ((Double) payload).doubleValue();
			} else if (payload instanceof Float) {
				return ((Float) payload).doubleValue();
			} else if (payload instanceof Long) {
				return ((Long) payload).doubleValue();
			} else if (payload instanceof String) {
				try {
					return Double.parseDouble((String) payload);
				} catch (Exception e) {
					throw new MessagingException("cannot convert payload to double", e);
				}
			}
		}
		throw new MessagingException("cannot convert " + payload == null ? "null" : payload.getClass().getName()
				+ " to double");
	}

}
