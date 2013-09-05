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

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.context.Lifecycle;
import org.springframework.core.convert.ConversionException;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.MediaType;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.x.json.TypedJsonMapper;
import org.springframework.util.Assert;
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

	protected static final String XD_JSON_OCTET_STREAM_VALUE =
			new MediaType("application", "x-xd-json-octet-stream").toString();

	protected static final String XD_TEXT_PLAIN_UTF8_VALUE =
			new MediaType("text", "x-xd-plain", Charset.forName("UTF-8")).toString();

	protected static final String XD_OCTET_STREAM_VALUE =
			new MediaType("application", "x-xd-octet-stream").toString();

	protected static final String ORIGINAL_CONTENT_TYPE_HEADER = "originalContentType";

	private final List<Bridge> bridges = Collections.synchronizedList(new ArrayList<Bridge>());

	public void setConversionService(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassloader = classLoader;
	}

	protected void addBridge(Bridge bridge) {
		this.bridges.add(bridge);
	}

	protected void deleteBridges(String name) {
		Assert.hasText(name, "a valid name is required to remove a bridge");
		synchronized (this.bridges) {
			Iterator<Bridge> iterator = this.bridges.iterator();
			while (iterator.hasNext()) {
				Bridge endpoint = iterator.next();
				if (endpoint.getEndpoint().getComponentName().equals(name)) {
					endpoint.stop();
					iterator.remove();
				}
			}
		}
	}

	protected void deleteBridge(String name, MessageChannel channel) {
		Assert.hasText(name, "a valid name is required to remove a bridge");
		Assert.notNull(channel, "A valid channel is required to remove a bridge");
		synchronized (this.bridges) {
			Iterator<Bridge> iterator = this.bridges.iterator();
			while (iterator.hasNext()) {
				Bridge channelEndpoint = iterator.next();
				if (channelEndpoint.getChannel().equals(channel) &&
						channelEndpoint.getEndpoint().getComponentName().equals(name)) {
					channelEndpoint.stop();
					iterator.remove();
					return;
				}
			}
		}
	}

	protected void stopBridges() {
		for (Lifecycle bean : this.bridges) {
			try {
				bean.stop();
			}
			catch (Exception e) {
				if (logger.isWarnEnabled()) {
					logger.warn("failed to stop adapter", e);
				}
			}
		}
	}

	protected final Message<?> transformOutboundIfNecessary(Message<?> message, MediaType to) {
		Message<?> messageToSend = message;
		Object originalPayload = message.getPayload();
		Object payload = null;
		Object originalContentType = message.getHeaders().get(MessageHeaders.CONTENT_TYPE);
		String originalContentTypeString = null;
		if (originalContentType instanceof MediaType) {
			originalContentTypeString = originalContentType.toString();
		}
		else if (originalContentType instanceof String) {
			originalContentTypeString = (String) originalContentType;
		}
		String contentType = originalContentTypeString;
		if (to.equals(MediaType.ALL)) {
			return message;
		}
		else if (to.equals(MediaType.APPLICATION_OCTET_STREAM)) {
			if (originalPayload instanceof byte[]) {
				payload = originalPayload;
				contentType = XD_OCTET_STREAM_VALUE;
			}
			else if (originalPayload instanceof String) {
				try {
					payload = ((String) originalPayload).getBytes("UTF-8");
					contentType = XD_TEXT_PLAIN_UTF8_VALUE;
				}
				catch (UnsupportedEncodingException e) {
					logger.error("Could not convert String to bytes", e);
				}
			}
			else {
				payload = this.jsonMapper.toBytes(originalPayload);
				contentType = XD_JSON_OCTET_STREAM_VALUE;
			}
		}
		else {
			throw new IllegalArgumentException("'to' can only be 'ALL' or 'APPLICATION_OCTET_STREAM'");
		}
		if (payload != null) {
			MessageBuilder<Object> messageBuilder = MessageBuilder.withPayload(payload)
					.copyHeaders(message.getHeaders())
					.setHeader(MessageHeaders.CONTENT_TYPE, contentType);
			if (originalContentTypeString != null) {
				messageBuilder.setHeader(ORIGINAL_CONTENT_TYPE_HEADER, originalContentTypeString);
			}
			messageToSend = messageBuilder.build();
		}
		return messageToSend;
	}

	protected final Message<?> transformInboundIfNecessary(Message<?> message, Collection<MediaType> acceptedMediaTypes) {
		Message<?> messageToSend = message;
		Object originalPayload = message.getPayload();
		String contentType = message.getHeaders().get(MessageHeaders.CONTENT_TYPE, String.class);
		Object payload = transformPayloadForInputChannel(originalPayload,
				contentType,
				acceptedMediaTypes);
		if (payload != null) {
			MessageBuilder<Object> transformed = MessageBuilder.withPayload(payload).copyHeaders(message.getHeaders());
			Object originalContentType = message.getHeaders().get(ORIGINAL_CONTENT_TYPE_HEADER);
			if (originalContentType != null) {
				transformed.setHeader(MessageHeaders.CONTENT_TYPE, originalContentType);
				transformed.setHeader(ORIGINAL_CONTENT_TYPE_HEADER, null);
			}
			else if (contentType != null && contentType.contains("/x-xd-")) {
				transformed.setHeader(MessageHeaders.CONTENT_TYPE, null);
			}
			messageToSend = transformed.build();
		}
		return messageToSend;
	}

	private Object transformPayloadForInputChannel(Object payload, String contentType, Collection<MediaType> to) {
		if (payload instanceof byte[]) {
			Object result = null;
			// Get the preferred java type, if any, and first try to decode directly from JSON.
			MediaType toObjectType = findJavaObjectType(to);
			if (XD_JSON_OCTET_STREAM_VALUE.equals(contentType)) {
				if (toObjectType == null || toObjectType.getParameter("type") == null) {
					try {
						result = this.jsonMapper.fromBytes((byte[]) payload);
					}
					catch (ConversionException e) {
						if (logger.isDebugEnabled()) {
							logger.debug("JSON decode failed, raw byte[]?");
						}
					}
				}
				else {
					String preferredClass = toObjectType.getParameter("type");
					try {
						// If this fails, fall back to generic decoding and delegate object conversion to the
						// conversionService
						result = this.jsonMapper.fromBytes((byte[]) payload, preferredClass);
					}
					catch (ConversionException e) {
						try {
							if (logger.isDebugEnabled()) {
								logger.debug("JSON decode failed to convert to requested type: " + preferredClass
										+ " - will try to decode to original type");
							}
							result = this.jsonMapper.fromBytes((byte[]) payload);
						}
						catch (ConversionException ce) {
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
						return transformPayloadForInputChannel(result, contentType,
								Collections.singletonList(toObjectType));
					}
				}
			}
			else if (XD_TEXT_PLAIN_UTF8_VALUE.equals(contentType)) {
				try {
					return new String((byte[]) payload, "UTF-8");
				}
				catch (UnsupportedEncodingException e) {
					logger.error("Could not convert bytes to String", e);
				}
			}
			else if (XD_OCTET_STREAM_VALUE.equals(contentType)) {
				return payload;
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
			else {
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
