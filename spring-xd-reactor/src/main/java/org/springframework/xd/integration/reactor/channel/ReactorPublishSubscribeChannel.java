package org.springframework.xd.integration.reactor.channel;

import org.springframework.integration.channel.AbstractSubscribableChannel;
import org.springframework.integration.dispatcher.MessageDispatcher;

/**
 * @author Jon Brisbin
 */
public class ReactorPublishSubscribeChannel extends AbstractSubscribableChannel {

	private final MessageDispatcher dispatcher;

	public ReactorPublishSubscribeChannel(MessageDispatcher dispatcher) {
		this.dispatcher = dispatcher;
	}

	@Override
	protected MessageDispatcher getDispatcher() {
		return dispatcher;
	}

}
