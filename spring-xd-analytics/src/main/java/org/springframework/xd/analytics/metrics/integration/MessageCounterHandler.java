package org.springframework.xd.analytics.metrics.integration;

import org.springframework.integration.Message;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.util.Assert;
import org.springframework.xd.analytics.metrics.core.CounterService;

/**
 * Counts the occurrence of a messages that are not null using an underlying CounterService
 * 
 */
public class MessageCounterHandler {

	private final CounterService counterService;
	private final String counterName;
	
	public MessageCounterHandler(CounterService counterService, String counterName) {
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
