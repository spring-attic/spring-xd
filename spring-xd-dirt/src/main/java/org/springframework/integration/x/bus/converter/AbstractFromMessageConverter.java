/*
 * Copyright 2014 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.x.bus.StrictContentTypeResolver;
import org.springframework.integration.x.bus.StringConvertingContentTypeResolver;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.AbstractMessageConverter;
import org.springframework.messaging.converter.ContentTypeResolver;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;


/**
 * Base class for converters applied via Spring Integration 4 data type channels
 * 
 * @author David Turanski
 */
public abstract class AbstractFromMessageConverter extends AbstractMessageConverter {

	protected Log logger = LogFactory.getLog(this.getClass());

	protected final List<MimeType> targetMimeTypes;

	/**
	 * Creates a converter that ignores content-type message headers
	 * 
	 * @param targetMimeType the required target type (outputType or inputType for XD)
	 */
	protected AbstractFromMessageConverter(MimeType targetMimeType) {
		this(new ArrayList<MimeType>(), targetMimeType, new StrictContentTypeResolver(targetMimeType));
	}

	protected AbstractFromMessageConverter(Collection<MimeType> targetMimeTypes) {
		this(new ArrayList<MimeType>(), targetMimeTypes, new StringConvertingContentTypeResolver());
	}

	/**
	 * Creates a converter that expects one or more content-type message headers
	 * 
	 * @param supportedSourceMimeTypes list of {@link MimeType} that may present in content-type header
	 * @param targetMimeType the required target type (outputType or inputType for XD)
	 */
	protected AbstractFromMessageConverter(Collection<MimeType> supportedSourceMimeTypes, MimeType targetMimeType,
			ContentTypeResolver contentTypeResolver) {
		super(supportedSourceMimeTypes);
		Assert.notNull(targetMimeType, "'targetMimeType' cannot be null");
		setContentTypeResolver(contentTypeResolver);
		this.targetMimeTypes = Collections.singletonList(targetMimeType);
	}

	protected AbstractFromMessageConverter(Collection<MimeType> supportedSourceMimeTypes,
			Collection<MimeType> targetMimeTypes,
			ContentTypeResolver contentTypeResolver) {
		super(supportedSourceMimeTypes);
		Assert.notNull(targetMimeTypes, "'targetMimeTypes' cannot be null");
		setContentTypeResolver(contentTypeResolver);
		this.targetMimeTypes = new ArrayList<MimeType>(targetMimeTypes);
	}

	/**
	 * Creates a converter that expects exactly one content-type message header
	 * 
	 * @param supportedSourceMimeTypes list of {@link MimeType} that may present in content-type header
	 * @param targetMimeType the required target type (outputType or inputType for XD)
	 */
	protected AbstractFromMessageConverter(MimeType supportedSourceMimeType, MimeType targetMimeType) {
		this(Collections.singletonList(supportedSourceMimeType), targetMimeType, new StrictContentTypeResolver(
				supportedSourceMimeType));
	}

	protected AbstractFromMessageConverter(MimeType supportedSourceMimeType, Collection<MimeType> targetMimeTypes) {
		this(Collections.singletonList(supportedSourceMimeType), targetMimeTypes, new StrictContentTypeResolver(
				supportedSourceMimeType));
	}

	protected abstract boolean supportsPayloadType(Class<?> clazz);

	@Override
	protected boolean canConvertFrom(Message<?> message, Class<?> targetClass) {
		return super.canConvertFrom(message, targetClass) && supportsPayloadType(message.getPayload().getClass());
	}

	public boolean supportsTargetMimeType(MimeType mimeType) {
		for (MimeType targetMimeType : targetMimeTypes) {
			if (mimeType.getType().equals(targetMimeType.getType()) && mimeType.getSubtype().equals(
					targetMimeType.getSubtype())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void setContentTypeResolver(ContentTypeResolver resolver) {
		if (getContentTypeResolver() == null) {
			super.setContentTypeResolver(resolver);
		}
	}

	@Override
	protected boolean canConvertTo(Object payload, MessageHeaders headers) {
		return false;
	}

	@Override
	public Object convertToInternal(Object payload, MessageHeaders headers) {
		throw new UnsupportedOperationException("'convertTo' not supported");
	}

	protected final Message<?> buildConvertedMessage(Object payload, MessageHeaders headers, MimeType contentType) {
		return MessageBuilder.withPayload(payload).copyHeaders(headers)
				.copyHeaders(
						Collections.singletonMap(MessageHeaders.CONTENT_TYPE,
								contentType)).build();
	}
}
