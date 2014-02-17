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

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.ClassUtils;

import org.springframework.integration.x.bus.StringConvertingContentTypeResolver;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.ContentTypeResolver;
import org.springframework.util.MimeType;


/**
 * 
 * @author David Turanski
 */
public class ByteArrayToStringMessageConverter extends AbstractFromMessageConverter {

	private final static ContentTypeResolver contentTypeResolver = new StringConvertingContentTypeResolver();

	private final static List<MimeType> targetMimeTypes = new ArrayList<MimeType>();
	static {
		targetMimeTypes.add(MimeType.valueOf("application/x-xd-string"));
		targetMimeTypes.add(
				MimeType.valueOf("application/x-java-object"));
	}

	public ByteArrayToStringMessageConverter() {
		super(targetMimeTypes);
	}

	@Override
	protected boolean supports(Class<?> clazz) {
		return ClassUtils.isAssignable(clazz, String.class);
	}

	@Override
	protected boolean supportsPayloadType(Class<?> clazz) {
		return (ClassUtils.isAssignable(clazz, byte[].class));
	}

	/**
	 * Don't need to manipulate message headers. Just return payload
	 */
	@Override
	public Object convertFromInternal(Message<?> message, Class<?> targetClass) {
		MimeType mimeType = contentTypeResolver.resolve(message.getHeaders());
		String converted = null;
		if (mimeType == null || mimeType.getParameter("Charset") == null) {
			try {
				converted = new String((byte[]) message.getPayload(), "UTF-8");
				return converted;
			}
			catch (UnsupportedEncodingException e) {
			}
		}
		String encoding = mimeType.getParameter("Charset");
		if (encoding != null) {
			try {
				converted = new String((byte[]) message.getPayload(), encoding);
			}
			catch (UnsupportedEncodingException e) {
				return null;
			}
		}
		return converted;

	}
}
