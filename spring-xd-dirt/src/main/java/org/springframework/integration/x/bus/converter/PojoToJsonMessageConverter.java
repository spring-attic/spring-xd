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

import org.apache.commons.lang.ClassUtils;

import org.springframework.messaging.Message;
import org.springframework.util.MimeTypeUtils;
import org.springframework.xd.tuple.Tuple;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;


/**
 * 
 * @author David Turanski
 */
public class PojoToJsonMessageConverter extends AbstractFromMessageConverter {

	private final ObjectMapper mapper = new ObjectMapper();

	public PojoToJsonMessageConverter() {
		super(MimeTypeUtils.APPLICATION_JSON);
		mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
	}

	@Override
	protected boolean supports(Class<?> clazz) {
		return ClassUtils.isAssignable(clazz, String.class);
	}

	@Override
	protected boolean supportsPayloadType(Class<?> clazz) {
		return !(ClassUtils.isAssignable(clazz, String.class) ||
				ClassUtils.isAssignable(clazz, byte[].class) || ClassUtils.isAssignable(clazz, Tuple.class));
	}


	@Override
	public Object convertFromInternal(Message<?> message, Class<?> targetClass) {
		Object result = null;
		try {
			result = mapper.writeValueAsString(message.getPayload());
		}
		catch (JsonProcessingException e) {
			logger.error(e.getMessage(), e);
			return null;
		}
		return buildConvertedMessage(result, message.getHeaders(), MimeTypeUtils.APPLICATION_JSON);
	}
}
