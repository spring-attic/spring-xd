/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.xd.dirt.plugins.job.support.listener;

import org.springframework.messaging.Message;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.xd.dirt.plugins.job.BatchJobHeaders;


/**
 * Abstract batch job listener that sends batch job listener payload to the {@link SubscribableChannel} notification
 * channel.
 * 
 * @author Ilayaperumal Gopinathan
 */
public abstract class BatchJobListener<P> {

	private final SubscribableChannel listenerEventsChannel;

	private final SubscribableChannel aggregatedEventsChannel;

	public BatchJobListener(SubscribableChannel listenerEventsChannel, SubscribableChannel aggregatedEventsChannel) {
		this.listenerEventsChannel = listenerEventsChannel;
		this.aggregatedEventsChannel = aggregatedEventsChannel;
	}

	protected final void publish(P payload) {
		if (payload instanceof Message) {
			this.publishMessage((Message<?>) payload);
			return;
		}
		Message<P> message = MessageBuilder.withPayload(payload).build();
		this.listenerEventsChannel.send(message);
		this.aggregatedEventsChannel.send(message);
	}

	private final void publishMessage(Message<?> message) {
		this.listenerEventsChannel.send(message);
		this.aggregatedEventsChannel.send(message);
	}

	protected void publishWithThrowableHeader(P payload, String header) {
		Message<P> message = MessageBuilder.withPayload(payload).setHeader(BatchJobHeaders.BATCH_EXCEPTION,
				header).build();
		publishMessage(message);
	}

}
