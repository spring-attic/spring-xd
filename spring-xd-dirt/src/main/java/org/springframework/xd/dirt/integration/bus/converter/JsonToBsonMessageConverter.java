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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.messaging.Message;
import org.springframework.util.MimeType;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.undercouch.bson4jackson.BsonFactory;


/**
 * A {@link MessageConverter} to convert from a JSON to a BSON.
 *
 * @author liujiong
 */
public class JsonToBsonMessageConverter extends AbstractFromMessageConverter {

	private final static List<MimeType> targetMimeTypes = new ArrayList<MimeType>();
	static {
		targetMimeTypes.add(MessageConverterUtils.X_XD_BSON);
	}

	public JsonToBsonMessageConverter() {
		super(targetMimeTypes);
	}

	@Override
	protected Class<?>[] supportedPayloadTypes() {
		return new Class<?>[] { String.class };
	}

	@Override
	protected Class<?>[] supportedTargetTypes() {
		return new Class<?>[] { byte[].class };
	}

	@Override
	public Object convertFromInternal(Message<?> message, Class<?> targetClass) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectMapper mapper = new ObjectMapper(new BsonFactory());
		try {
			mapper.writeValue(baos, message.getPayload());
		}
		catch (IOException e) {
			logger.error(e.getMessage(), e);
			return null;
		}
		return buildConvertedMessage(baos.toByteArray(), message.getHeaders(), MessageConverterUtils.X_XD_BSON);
	}
}
