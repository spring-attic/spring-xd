
package org.springframework.xd.analytics.metrics.integration;

import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.xd.analytics.metrics.core.CounterRepository;

/**
 * Counts the number of non-null messages using an underlying {@link CounterRepository}.
 * 
 */
public class MessageCounterHandler {

	private final CounterRepository counterRepository;

	private final String counterName;

	public MessageCounterHandler(CounterRepository counterRepository, String counterName) {
		Assert.notNull(counterRepository, "Counter Repository can not be null");
		Assert.notNull(counterName, "Counter Name can not be null");
		this.counterRepository = counterRepository;
		this.counterName = counterName;
	}

	@ServiceActivator
	public Message<?> process(Message<?> message) {
		if (message != null) {
			this.counterRepository.increment(counterName);
		}
		return message;
	}

}
