/*
 * Copyright 2013 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.http.MediaType;
import org.springframework.integration.Message;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.xd.module.Module;

/**
 * @author David Turanski
 * @author Gary Russell
 *
 */
public abstract class ChannelRegistrySupport implements ChannelRegistry {

	protected Log logger = LogFactory.getLog(getClass());

	protected final Message<?> transformInboundIfNecessary(Message<?> message, Module module) {
		Message<?> messageToSend = message;
		MediaType from = headerToMediaType(message.getHeaders().get(MessageHeaders.CONTENT_TYPE));
		Object originalPayload = message.getPayload();
		Object payload = transformPayload(originalPayload, from, module.getAcceptedMediaTypes());
		if (payload != null && payload != originalPayload) {
			messageToSend = MessageBuilder.withPayload(payload).copyHeaders(message.getHeaders()).build();
		}
		return messageToSend;
	}

	protected final Message<?> transformOutboundIfNecessary(Message<?> message, MediaType to) {
		// TODO: should Modules also support "produces" (like "accepts") so we don't need a header.
		Message<?> messageToSend = message;
		MediaType from = headerToMediaType(message.getHeaders().get(MessageHeaders.CONTENT_TYPE));
		Object originalPayload = message.getPayload();
		Object payload = transformPayload(originalPayload, from, to);
		if (payload != null && payload != originalPayload) {
			messageToSend = MessageBuilder.withPayload(payload).copyHeaders(message.getHeaders()).build();
		}
		return messageToSend;
	}

	protected Object transformPayload(Object payload, MediaType from, MediaType to) {
		if (to == MediaType.ALL) {
			return payload;
		}
		// TODO
		return null;
	}

	/**
	 * @param payload
	 * @param from
	 * @param to
	 * @return null if payload can't be transformed
	 */
	protected Object transformPayload(Object payload, MediaType from, Collection<MediaType> to) {
		if (to.contains(MediaType.ALL)) {
			return payload;
		}
		// TODO
		return null;
	}

	protected MediaType headerToMediaType(Object header) {
		// TODO
		return MediaType.ALL;
	}

}
