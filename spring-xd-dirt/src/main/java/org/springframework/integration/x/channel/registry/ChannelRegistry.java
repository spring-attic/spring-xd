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

package org.springframework.integration.x.channel.registry;

import java.util.Collection;

import org.springframework.http.MediaType;
import org.springframework.integration.MessageChannel;

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
public interface ChannelRegistry {

	/**
	 * Register a message consumer on a p2p channel
	 * 
	 * @param name the logical identity of the message source
	 * @param moduleInputChannel the channel bound as a consumer
	 * @param acceptedMediaTypes the media types supported by the channel
	 * @param aliasHint whether the provided name represents an alias and thus should support late binding
	 */
	void createInbound(String name, MessageChannel moduleInputChannel, Collection<MediaType> acceptedMediaTypes,
			boolean aliasHint);


	/**
	 * Register a message consumer on a pub/sub channel
	 * 
	 * @param name the logical identity of the message source
	 * @param moduleInputChannel the channel bound as a pub/sub consumer
	 * @param acceptedMediaTypes the media types supported by the channel
	 */
	void createInboundPubSub(final String name, MessageChannel inputChannel,
			final Collection<MediaType> acceptedMediaTypes);

	/**
	 * Register a message producer on a p2p channel.
	 * 
	 * @param name the logical identity of the message target
	 * @param moduleOutputChannel the channel bound as a producer
	 * @param aliasHint whether the provided name represents an alias and thus should support late binding
	 */
	void createOutbound(String name, MessageChannel moduleOutputChannel, boolean aliasHint);


	/**
	 * Register a message producer on a pub/sub channel.
	 * 
	 * @param name the logical identity of the message target
	 * @param moduleOutputChannel the channel bound as a producer
	 */
	void createOutboundPubSub(final String name, MessageChannel outputChannel);

	/**
	 * Remove an inbound inter-module channel and stop any active components that use the channel.
	 * 
	 * @param name the channel name
	 */
	void deleteInbound(String name);

	/**
	 * Remove an outbound inter-module channel and stop any active components that use the channel.
	 * 
	 * @param name the channel name
	 */
	void deleteOutbound(String name);

	/**
	 * Unregister a specific p2p or pub/sub message consumer
	 * 
	 * @param name The logical identify of a message source
	 * @param channel The channel bound as a consumer
	 */
	void deleteInbound(String name, MessageChannel channel);

	/**
	 * Unregister a specific p2p or pub/sub message producer
	 * 
	 * @param name the logical identity of the message target
	 * @param moduleOutputChannel the channel bound as a producer
	 */
	void deleteOutbound(String name, MessageChannel channel);
}
