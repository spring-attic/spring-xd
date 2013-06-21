/*
 * Copyright 2002-2011 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.springframework.integration.flow.handler;

import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationContextEvent;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.MessagingException;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.core.SubscribableChannel;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.message.ErrorMessage;
import org.springframework.integration.support.MessageBuilder;

/**
 * @author David Turanski
 * @since 3.0
 * 
 */
public class FlowExecutingMessageHandler extends AbstractReplyProducingMessageHandler implements
		ApplicationListener<ApplicationContextEvent> {

	private static Log log = LogFactory.getLog(FlowExecutingMessageHandler.class);

	private volatile MessageChannel flowInputChannel;

	private volatile SubscribableChannel flowOutputChannel;

	private volatile MessageChannel errorChannel;

	private long timeout;

	private final String flowId;

	public FlowExecutingMessageHandler(String flowId) {
		this.flowId = flowId;
	}

	public void setErrorChannel(MessageChannel errorChannel) {
		this.errorChannel = errorChannel;
	}

	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	@Override
	protected Object handleRequestMessage(Message<?> requestMessage) {
		UUID conversationId = requestMessage.getHeaders().getId();
		Message<?> message = MessageBuilder.fromMessage(requestMessage).pushSequenceDetails(conversationId, 0, 0)
				.build();
		try {

			ResponseMessageHandler responseMessageHandler = new ResponseMessageHandler(conversationId);
			flowOutputChannel.subscribe(responseMessageHandler);
			flowInputChannel.send(message, timeout);
			flowOutputChannel.unsubscribe(responseMessageHandler);

			return responseMessageHandler.getResponse();

		} catch (MessagingException me) {
			log.error(me.getMessage(), me);

			if (conversationId.equals(me.getFailedMessage().getHeaders().getCorrelationId())) {
				if (errorChannel != null) {
					errorChannel.send(new ErrorMessage(me));

				}
			} else {
				throw me;
			}
		}
		return null;
	}

	/*
	 * Internal MessageHandler for the flow response
	 */
	private static class ResponseMessageHandler implements MessageHandler {
		private final UUID correlationId;

		private volatile Message<?> response;

		public ResponseMessageHandler(UUID correlationId) {
			this.correlationId = correlationId;
		}

		public void handleMessage(Message<?> message) throws MessagingException {
			if (correlationId.equals(message.getHeaders().getCorrelationId())) {
				this.response = MessageBuilder.fromMessage(message).popSequenceDetails().build();
			} else {

				if (message instanceof ErrorMessage) {
					MessagingException me = (MessagingException) message.getPayload();
					if (correlationId.equals(me.getFailedMessage().getHeaders().getCorrelationId())) {
						this.response = message;
					}
				}
			}
		}

		public Message<?> getResponse() {
			return this.response;
		}
	}

	@Override
	public void onApplicationEvent(ApplicationContextEvent event) {
		ApplicationContext ctx = event.getApplicationContext();
		try {
			this.flowInputChannel = ctx.getBean(this.flowId + ".input", MessageChannel.class);
		} catch (RuntimeException e) {
			log.error("No Message Channel bean named '" + this.flowId + ".input' is defined. Is the flow '" + this.flowId + "' declared?");
			throw e;
		}
		if (ctx.containsBean(this.flowId + ".output")) {
			this.flowOutputChannel = ctx.getBean(this.flowId + ".output", SubscribableChannel.class);
		}
	}

}
