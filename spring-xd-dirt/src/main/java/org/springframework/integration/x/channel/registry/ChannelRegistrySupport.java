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

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.core.convert.ConversionException;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.MediaType;
import org.springframework.integration.Message;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.x.json.TypedJsonMapper;
import org.springframework.util.ClassUtils;

/**
 * @author David Turanski
 * @author Gary Russell
 *
 */
public abstract class ChannelRegistrySupport implements ChannelRegistry, BeanClassLoaderAware {

	protected final Log logger = LogFactory.getLog(getClass());

	private volatile ConversionService conversionService;

	private final TypedJsonMapper jsonMapper = new TypedJsonMapper();

	private volatile ClassLoader beanClassloader = ClassUtils.getDefaultClassLoader();

	private static final MediaType JAVA_OBJECT_TYPE = new MediaType("application", "x-java-object");

	public void setConversionService(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassloader = classLoader;
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
		} else if (to.equals(MediaType.APPLICATION_OCTET_STREAM)) {
			if (payload instanceof byte[]) {
				return payload;
			} else {
				return this.jsonMapper.toBytes(payload);
			}
		} else {
			throw new IllegalArgumentException("'to' can only be 'ALL' or 'APPLICATION_OCTET_STREAM'");
		}
	}

	protected final Message<?> transformInboundIfNecessary(Message<?> message, Collection<MediaType> acceptedMediaTypes) {
		Message<?> messageToSend = message;
		Object originalPayload = message.getPayload();
		Object payload = transformPayloadForInputChannel(originalPayload, acceptedMediaTypes);
		if (payload != null && payload != originalPayload) {
			messageToSend = MessageBuilder.withPayload(payload).copyHeaders(message.getHeaders()).build();
		}
		return messageToSend;
	}

	private Object transformPayloadForInputChannel(Object payload, Collection<MediaType> to) {
		// TODO need a way to short circuit the JSON decode attempt when it's a straight byte[] pass thru
		// Perhaps a module property (that we'd have to pass in somehow).
		// Perhaps a 'Special' MediaType "application/x-xd-raw"  ??
		if (payload instanceof byte[]) {
			Object result = null;
			// Get the preferred java type, if any, and first try to decode directly from JSON.
			MediaType toObjectType = findJavaObjectType(to);
			if (toObjectType == null || toObjectType.getParameter("type") == null) {
				try {
					result = this.jsonMapper.fromBytes((byte[]) payload);
				} catch (ConversionException e) {
					if (logger.isDebugEnabled()) {
						logger.debug("JSON decode failed, raw byte[]?");
					}
				}
			} else {
				String preferredClass = toObjectType.getParameter("type");
				try {
					//If this fails, fall back to generic decoding and delegate object conversion to the conversionService
					result = this.jsonMapper.fromBytes((byte[]) payload, preferredClass);
				} catch (ConversionException e) {
					try {
						if (logger.isDebugEnabled()) {
							logger.debug("JSON decode failed to convert to requested type: " + preferredClass+ " - will try to decode to original type");
						}
						result = this.jsonMapper.fromBytes((byte[]) payload);
					} catch (ConversionException ce) {
						if (logger.isDebugEnabled()) {
							logger.debug("JSON decode failed, raw byte[]?");
						}
					}
				}
			}

			if (result != null) {
				if (to.contains(MediaType.ALL)) {
					return result;
				}
				// TODO: currently only tries the first application/x-java-object;type=foo.Foo
				toObjectType = findJavaObjectType(to);
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
					clazz = this.beanClassloader.loadClass(requiredType);
				} catch (ClassNotFoundException e) {
					if (logger.isDebugEnabled()) {
						logger.debug("Class not found", e);
					}
				}
				if (clazz != null) {
					if (this.conversionService.canConvert(payload.getClass(), clazz)) {
						return this.conversionService.convert(payload, clazz);
					}
				}
			} else {
				if (this.acceptsString(to)) {
					if (this.conversionService.canConvert(payload.getClass(), String.class)) {
						return this.conversionService.convert(payload, String.class);
					}
				}
			}
		}
		return null;
	}

	private MediaType findJavaObjectType(Collection<MediaType> to) {
		MediaType toObjectType = null;
		for (MediaType mediaType : to) {
			if (JAVA_OBJECT_TYPE.includes(mediaType)) {
				return mediaType;
			}
		}
		return toObjectType;
	}

	private boolean acceptsString(Collection<MediaType> to) {
		for (MediaType mediaType : to) {
			if (mediaType.getType().equals("text")) {
				return true;
			}
		}
		return false;
	}
}
