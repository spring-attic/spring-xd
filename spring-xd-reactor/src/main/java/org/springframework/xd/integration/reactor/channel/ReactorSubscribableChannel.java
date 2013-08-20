package org.springframework.xd.integration.reactor.channel;

import org.springframework.integration.channel.AbstractSubscribableChannel;
import org.springframework.integration.dispatcher.MessageDispatcher;
import org.springframework.xd.integration.reactor.dispatcher.ReactorProcessorMessageDispatcher;

/**
 * @author Jon Brisbin
 */
public class ReactorSubscribableChannel extends AbstractSubscribableChannel {

	private final ReactorProcessorMessageDispatcher dispatcher;

	public ReactorSubscribableChannel(ReactorProcessorMessageDispatcher dispatcher) {
		this.dispatcher = dispatcher;
	}

	@Override
	protected MessageDispatcher getDispatcher() {
		return dispatcher;
	}

}
