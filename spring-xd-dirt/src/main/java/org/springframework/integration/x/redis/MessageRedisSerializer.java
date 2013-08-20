/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.x.redis;

import java.util.Map;

import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.integration.Message;
import org.springframework.integration.support.MessageBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Implementation of {@link RedisSerializer} that uses {@link MessageBuilder} to
 * deserialize raw bytes into {@link Message}s
 *
 * @author Jennifer Hickey
 */
public class MessageRedisSerializer implements RedisSerializer<Message<?>> {

	private ObjectMapper objectMapper = new ObjectMapper();

	private RedisSerializer<String> stringSerializer = new StringRedisSerializer();

	@Override
	public byte[] serialize(Message<?> message) throws SerializationException {
		try {
			return stringSerializer.serialize(this.objectMapper.writeValueAsString(message));
		}
		catch (JsonProcessingException e) {
			throw new SerializationException("Error writing Message to String", e);
		}
	}

	@Override
	public Message<?> deserialize(byte[] bytes) throws SerializationException {
		if (bytes == null) {
			return null;
		}
		try {
			MessageDeserializationWrapper wrapper = objectMapper.readValue(stringSerializer.deserialize(bytes),
					MessageDeserializationWrapper.class);
			return wrapper.getMessage();
		}
		catch (Exception e) {
			throw new SerializationException("Error deserializing Message", e);
		}
	}

	/**
	 *
	 * @param stringSerializer The {@link RedisSerializer} to use for converting bytes
	 *        to/from Strings before further serialization
	 */
	public void setStringSerializer(RedisSerializer<String> stringSerializer) {
		this.stringSerializer = stringSerializer;
	}

	/**
	 * DTO class for de-serialization, relying on {@link MessageBuilder} to return the
	 * correct message type. Also, serialization framework needs a no-arg constructor.
	 *
	 * @author Jennifer Hickey
	 */
	private static class MessageDeserializationWrapper {

		private volatile Map<String, Object> headers;

		private volatile Object payload;

		@SuppressWarnings("unused")
		void setHeaders(Map<String, Object> headers) {
			this.headers = headers;
		}

		@SuppressWarnings("unused")
		void setPayload(Object payload) {
			this.payload = payload;
		}

		Message<?> getMessage() {
			return MessageBuilder.withPayload(this.payload).copyHeaders(this.headers).build();
		}
	}
}
