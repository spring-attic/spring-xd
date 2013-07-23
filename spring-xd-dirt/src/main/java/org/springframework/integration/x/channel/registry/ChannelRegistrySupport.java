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
import java.util.Collections;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.convert.ConversionException;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.MediaType;
import org.springframework.integration.Message;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.x.json.TypedJsonMapper;
import org.springframework.xd.module.Module;

/**
 * @author David Turanski
 * @author Gary Russell
 *
 */
public abstract class ChannelRegistrySupport implements ChannelRegistry {

	protected final Log logger = LogFactory.getLog(getClass());

	private volatile ConversionService conversionService;

	private final TypedJsonMapper jsonMapper = new TypedJsonMapper();

	private final MediaType javaObjectType = new MediaType("application", "x-java-object");

	public void setConversionService(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	protected final Message<?> transformOutboundIfNecessary(Message<?> message, MediaType to) {
		Message<?> messageToSend = message;
		Object originalPayload = message.getPayload();
		Object payload = transformPayloadFromOutputChannel(originalPayload, to);
		if (payload != null && payload != originalPayload) {
			messageToSend = MessageBuilder.withPayload(payload).copyHeaders(message.getHeaders()).build();
		}
		return messageToSend;
	}

	private Object transformPayloadFromOutputChannel(Object payload, MediaType to) {
		if (to.equals(MediaType.ALL)) {
			return payload;
		}
		else if (to.equals(MediaType.APPLICATION_OCTET_STREAM)) {
			if (payload instanceof byte[]) {
				return payload;
			}
			else {
				return this.jsonMapper.toBytes(payload);
			}
		}
		else {
			throw new IllegalArgumentException("'to' can only be 'ALL' or 'APPLICATION_OCTET_STREAM'");
		}
	}

	protected final Message<?> transformInboundIfNecessary(Message<?> message, Module module) {
		Message<?> messageToSend = message;
		Object originalPayload = message.getPayload();
		Object payload = transformPayloadForInputChannel(originalPayload, module.getAcceptedMediaTypes());
		if (payload != null && payload != originalPayload) {
			messageToSend = MessageBuilder.withPayload(payload).copyHeaders(message.getHeaders()).build();
		}
		return messageToSend;
	}

	private Object transformPayloadForInputChannel(Object payload, Collection<MediaType> to) {
		// TODO need a way to short circuit the JSON decode attempt when it's a straight byte[] pass thru
		if (payload instanceof byte[]) {
			Object result = null;
			try {
				result = this.jsonMapper.fromBytes((byte[]) payload);
			}
			catch (ConversionException e) {
				if (logger.isDebugEnabled()) {
					logger.debug("JSON decode failed, raw byte[]?");
				}
			}
			if (result != null) {
				if (to.contains(MediaType.ALL)) {
					return result;
				}
				// TODO: currently only tries the first application/x-java-object;type=foo.Foo
				MediaType toObjectType = findJavaObjectType(to);
				if (toObjectType != null) {
					if (toObjectType.getParameter("type") == null) {
						return result;
					}
					String resultClass = result.getClass().getName();
					if (resultClass.equals(toObjectType.getParameter("type"))) {
						return result;
					}
					// recursive call
					return transformPayloadForInputChannel(result, Collections.singletonList(toObjectType));
				}
			}
		}
		if (to.contains(MediaType.ALL)) {
			return payload;
		}
		return convert(payload, to);
	}

	private Object convert(Object payload, Collection<MediaType> to) {
		if (this.conversionService != null) {
			MediaType requiredMediaType = findJavaObjectType(to);
			if (requiredMediaType != null) {
				String requiredType = requiredMediaType.getParameter("type");
				if (requiredType == null) {
					return payload;
				}
				Class<?> clazz = null;
				try {
					clazz = Class.forName(requiredType);
				}
				catch (ClassNotFoundException e) {
					if (logger.isDebugEnabled()) {
						logger.debug("Class not found", e);
					}
				}
				if (clazz != null) {
					if (this.conversionService.canConvert(payload.getClass(), clazz)) {
						return this.conversionService.convert(payload, clazz);
					}
				}
			}
		}
		return null;
	}

	private MediaType findJavaObjectType(Collection<MediaType> to) {
		MediaType toObjectType = null;
		for (MediaType mediaType : to) {
			if (mediaType.includes(this.javaObjectType)) {
				toObjectType = mediaType;
			}
		}
		return toObjectType;
	}

}
