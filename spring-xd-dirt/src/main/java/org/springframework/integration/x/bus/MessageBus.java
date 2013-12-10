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

package org.springframework.integration.x.bus;

import java.util.Collection;

import org.springframework.http.MediaType;
import org.springframework.messaging.MessageChannel;

/**
 * A strategy interface used to bind a {@link MessageChannel} to a logical name. The name is intended to identify a
 * logical consumer or producer of messages. This may be a queue, a channel adapter, another message channel, a Spring
 * bean, etc.
 * 
 * @author Mark Fisher
 * @author David Turanski
 * @author Gary Russell
 * @author Jennifer Hickey
 * @since 1.0
 */
public interface MessageBus {

	/**
	 * Bind a message consumer on a p2p channel
	 * 
	 * @param name the logical identity of the message source
	 * @param moduleInputChannel the channel bound as a consumer
	 * @param acceptedMediaTypes the media types supported by the channel
	 * @param aliasHint whether the provided name represents an alias and thus should support late binding
	 */
	void bindConsumer(String name, MessageChannel moduleInputChannel, Collection<MediaType> acceptedMediaTypes,
			boolean aliasHint);


	/**
	 * Bind a message consumer on a pub/sub channel
	 * 
	 * @param name the logical identity of the message source
	 * @param inputChannel the channel bound as a pub/sub consumer
	 * @param acceptedMediaTypes the media types supported by the channel
	 */
	void bindPubSubConsumer(final String name, MessageChannel inputChannel,
			final Collection<MediaType> acceptedMediaTypes);

	/**
	 * Bind a message producer on a p2p channel.
	 * 
	 * @param name the logical identity of the message target
	 * @param moduleOutputChannel the channel bound as a producer
	 * @param aliasHint whether the provided name represents an alias and thus should support late binding
	 */
	void bindProducer(String name, MessageChannel moduleOutputChannel, boolean aliasHint);


	/**
	 * Bind a message producer on a pub/sub channel.
	 * 
	 * @param name the logical identity of the message target
	 * @param outputChannel the channel bound as a producer
	 */
	void bindPubSubProducer(final String name, MessageChannel outputChannel);

	/**
	 * Unbind an inbound inter-module channel and stop any active components that use the channel.
	 * 
	 * @param name the channel name
	 */
	void unbindConsumers(String name);

	/**
	 * Unbind an outbound inter-module channel and stop any active components that use the channel.
	 * 
	 * @param name the channel name
	 */
	void unbindProducers(String name);

	/**
	 * Unbind a specific p2p or pub/sub message consumer
	 * 
	 * @param name The logical identify of a message source
	 * @param channel The channel bound as a consumer
	 */
	void unbindConsumer(String name, MessageChannel channel);

	/**
	 * Unbind a specific p2p or pub/sub message producer
	 * 
	 * @param name the logical identity of the message target
	 * @param channel the channel bound as a producer
	 */
	void unbindProducer(String name, MessageChannel channel);

	/**
	 * Bind a producer that expects async replies. To unbind, invoke unbindProducer() and unbindConsumer().
	 * 
	 * @param name The name of the requestor.
	 * @param requests The request channel - sends requests.
	 * @param replies The reply channel - receives replies.
	 */
	void bindRequestor(String name, MessageChannel requests, MessageChannel replies);

	/**
	 * Bind a consumer that handles requests from a requestor and asynchronously sends replies. To unbind, invoke
	 * unbindProducer() and unbindConsumer().
	 * 
	 * @param name The name of the requestor for which this replier will handle requests.
	 * @param requests The request channel - receives requests.
	 * @param replies The reply channel - sends replies.
	 */
	void bindReplier(String name, MessageChannel requests, MessageChannel replies);

}
