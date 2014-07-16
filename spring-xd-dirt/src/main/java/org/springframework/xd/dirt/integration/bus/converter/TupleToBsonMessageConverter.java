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

import java.util.ArrayList;
import java.util.List;

import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.util.MimeType;
import org.springframework.xd.tuple.Tuple;


/**
 * An {@link MessageConverter} to convert a {@link Tuple} to a BSON String
 *
 * @author liujiong
 */
public class TupleToBsonMessageConverter extends AbstractFromMessageConverter {

	private final static List<MimeType> targetMimeTypes = new ArrayList<MimeType>();
	static {
		targetMimeTypes.add(MessageConverterUtils.X_XD_BSON);
	}

	public TupleToBsonMessageConverter() {
		super(targetMimeTypes);
	}

	@Override
	protected Class<?>[] supportedTargetTypes() {
		return new Class<?>[] { byte[].class };
	}

	@Override
	protected Class<?>[] supportedPayloadTypes() {
		return new Class<?>[] { Tuple.class };
	}

	@Override
	public Object convertFromInternal(Message<?> message, Class<?> targetClass) {
		Tuple t = (Tuple) message.getPayload();
		String json = t.toString();

		Message<?> msg = MessageBuilder.withPayload(json).build();
		JsonToBsonMessageConverter converter = new JsonToBsonMessageConverter();
		Message<byte[]> result = (Message<byte[]>) converter.fromMessage(msg, byte[].class);
		return buildConvertedMessage(result.getPayload(), message.getHeaders(), MessageConverterUtils.X_XD_BSON);
	}

}
