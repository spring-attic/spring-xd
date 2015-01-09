/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.integration.x.http;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.handler.codec.http.HttpRequest;

import org.springframework.http.MediaType;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.integration.support.DefaultMessageBuilderFactory;
import org.springframework.integration.support.MessageBuilderFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.util.Assert;


/**
 * Message converter (inbound only) to convert a Netty Http MessageEvent to
 * a Message. Returns null if the content is not readable.
 *
 * @author Mark Fisher
 * @author Jennifer Hickey
 * @author Gary Russell
 * @author Marius Bogoevici
 * @author Peter Rietzler
 */
public class NettyInboundMessageConverter implements MessageConverter {

	private final MessageBuilderFactory messageBuilderFactory;

	public NettyInboundMessageConverter() {
		this(new DefaultMessageBuilderFactory());
	}

	public NettyInboundMessageConverter(MessageBuilderFactory messageBuilderFactory) {
		this.messageBuilderFactory = messageBuilderFactory;
	}

	@Override
	public Object fromMessage(Message<?> message, Class<?> targetClass) {
		throw new UnsupportedOperationException("This converter is for inbound messages only.");
	}

	@Override
	public Message<?> toMessage(Object payload, MessageHeaders header) {
		Assert.isInstanceOf(HttpRequest.class, payload);
		HttpRequest request = (HttpRequest) payload;
		ChannelBuffer content = request.getContent();
		Charset charsetToUse = null;
		boolean binary = false;
		if (content.readable()) {
			Map<String, String> messageHeaders = new HashMap<String, String>();
			for (Entry<String, String> entry : request.getHeaders()) {
				if (entry.getKey().equalsIgnoreCase("Content-Type")) {
					MediaType contentType = MediaType.parseMediaType(entry.getValue());
					charsetToUse = contentType.getCharSet();
					messageHeaders.put(MessageHeaders.CONTENT_TYPE, entry.getValue());
					binary = MediaType.APPLICATION_OCTET_STREAM.equals(contentType);
				}
				else if (!entry.getKey().toUpperCase().startsWith("ACCEPT")
						&& !entry.getKey().toUpperCase().equals("CONNECTION")) {
					messageHeaders.put(entry.getKey(), entry.getValue());
				}
			}
			messageHeaders.put("requestPath", request.getUri());
			messageHeaders.put("requestMethod", request.getMethod().toString());
			addHeaders(messageHeaders, request);
			try {
				AbstractIntegrationMessageBuilder<?> builder;
				if (binary) {
					builder = this.messageBuilderFactory.withPayload(toByteArray(content));
				}
				else {
					// ISO-8859-1 is the default http charset when not set
					charsetToUse = charsetToUse == null ? Charset.forName("ISO-8859-1") : charsetToUse;
					builder = this.messageBuilderFactory.withPayload(content.toString(charsetToUse));
				}
				builder.copyHeaders(messageHeaders);
				return builder.build();
			}
			catch (Exception ex) {
				throw new MessageConversionException("Failed to convert netty event to a Message", ex);
			}
		}
		else {
			return null;
		}
	}

	private byte[] toByteArray(ChannelBuffer content) {
		if (content.hasArray()) {
			return content.array();
		}
		else {
			byte[] bytes = new byte[content.readableBytes()];
			content.getBytes(0, bytes);
			return bytes;
		}
	}

	/**
	 * Add additional headers. Default implementation adds none.
	 * @param messageHeaders The headers that will be added to the message.
	 * @param request The HttpRequest
	 */
	protected void addHeaders(Map<String, String> messageHeaders, HttpRequest request) {
	}

}
