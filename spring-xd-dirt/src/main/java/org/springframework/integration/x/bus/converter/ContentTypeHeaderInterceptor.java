/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.integration.x.bus.converter;

import java.util.Collections;

import org.springframework.http.MediaType;
import org.springframework.integration.channel.interceptor.ChannelInterceptorAdapter;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.x.bus.DefaultMessageMediaTypeResolver;
import org.springframework.integration.x.bus.MessageMediaTypeResolver;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;


/**
 * 
 * @author David Turanski
 */
public class ContentTypeHeaderInterceptor extends ChannelInterceptorAdapter {

	private final MediaType contentType;

	private final MessageMediaTypeResolver messageMediaTypeResolver = new DefaultMessageMediaTypeResolver();

	public ContentTypeHeaderInterceptor(MediaType contentType) {
		this.contentType = contentType;
	}

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		return enrichMessage(message);
	}

	private Message<?> enrichMessage(Message<?> message) {
		if (this.contentType.equals(messageMediaTypeResolver.resolveMediaType(message))) {
			return message;
		}
		return MessageBuilder.fromMessage(message).copyHeaders(
				Collections.singletonMap(MessageHeaders.CONTENT_TYPE, this.contentType))
				.build();
	}
}
