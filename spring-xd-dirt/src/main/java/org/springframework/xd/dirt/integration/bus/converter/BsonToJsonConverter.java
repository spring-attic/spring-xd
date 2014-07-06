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

import org.bson.BSONObject;
import org.bson.BasicBSONDecoder;

import org.springframework.messaging.Message;
import org.springframework.util.MimeTypeUtils;


/**
 * A {@link MessageConverter} to convert from a JSON (byte[] or String) to a BSON.
 *
 * @author liujiong
 */
public class BsonToJsonConverter extends AbstractFromMessageConverter {

	//	public final static String APPLICATION_BSON_VALUE = "application/bson";
	//
	//	private final static List<MimeType> targetMimeTypes = new ArrayList<MimeType>();
	//
	//	private static final MimeType bson = MimeType.valueOf(APPLICATION_BSON_VALUE);
	//	static {
	//		targetMimeTypes.add(MessageConverterUtils.JSON);
	//	}

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
		byte[] source = (byte[]) message.getPayload();
		BasicBSONDecoder decoder = new BasicBSONDecoder();
		BSONObject bsonObject = decoder.readObject(source);
		String json_string = bsonObject.toString();
		return buildConvertedMessage(json_string, message.getHeaders(), MimeTypeUtils.APPLICATION_JSON);
	}
}
