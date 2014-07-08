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
import java.io.StringWriter;

import org.springframework.messaging.Message;
import org.springframework.util.MimeTypeUtils;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import de.undercouch.bson4jackson.BsonFactory;


/**
 * A {@link MessageConverter} to convert from a JSON (byte[] or String) to a BSON.
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
		BsonFactory factory = new BsonFactory();
		ByteArrayInputStream bais = new ByteArrayInputStream((byte[]) message.getPayload());
		JsonParser parser;
		StringWriter writer = new StringWriter();
		try {
			parser = factory.createJsonParser(bais);
			parser.nextToken();
			JsonFactory jfactory = new JsonFactory();

			JsonGenerator jGenerator = jfactory.createGenerator(writer);
			jGenerator.writeStartObject();
			while (parser.nextToken() != JsonToken.END_OBJECT) {
				String fieldname = parser.getCurrentName();
				parser.nextToken();
				try {
					float f = Float.parseFloat(parser.getText());
					jGenerator.writeNumberField(fieldname, f);
				}
				catch (NumberFormatException e) {
					jGenerator.writeStringField(fieldname, parser.getText());
				}
			}
			jGenerator.writeEndObject();
			jGenerator.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return buildConvertedMessage(writer.toString(), message.getHeaders(), MimeTypeUtils.APPLICATION_JSON);
	}
}
