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

package org.springframework.xd.dirt.integration.bus.converter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;

import org.springframework.messaging.Message;
import org.springframework.util.MimeTypeUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.undercouch.bson4jackson.BsonFactory;


/**
 * A {@link MessageConverter} to convert from a BSON (byte[] or String) to a JSON.
 *
 * @author liujiong
 */
public class BsonToJsonConverter extends AbstractFromMessageConverter {

	public BsonToJsonConverter() {
		super(MimeTypeUtils.APPLICATION_JSON);
	}

	@Override
	protected Class<?>[] supportedTargetTypes() {
		return new Class<?>[] { String.class };
	}

	@Override
	protected Class<?>[] supportedPayloadTypes() {
		return new Class<?>[] { byte[].class };
	}

	@Override
	public Object convertFromInternal(Message<?> message, Class<?> targetClass) {
		ByteArrayInputStream bais = new ByteArrayInputStream((byte[]) message.getPayload());
		String mapAsJson = null;
		ObjectMapper mapper = new ObjectMapper(new BsonFactory());
		Map readValue;
		try {
			readValue = mapper.readValue(bais, Map.class);
			mapAsJson = new ObjectMapper().writeValueAsString(readValue);
		}
		catch (IOException e) {
			logger.error(e.getMessage(), e);
			return null;
		}
		return buildConvertedMessage(mapAsJson, message.getHeaders(), MimeTypeUtils.APPLICATION_JSON);
	}
}
