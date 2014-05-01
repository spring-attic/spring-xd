/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.xd.dirt.integration.redis;

import java.util.HashMap;
import java.util.Map;

import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageHeaderAccessor;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;


/**
 * Implementation of Kryo Serializer to handle {@link MessageHeaders} which is immutable.
 * 
 * @author David Turanski
 * @since 1.0
 */
public class KryoMessageHeadersSerializer extends Serializer<MessageHeaders> {

	@Override
	public void write(Kryo kryo, Output output, MessageHeaders headers) {
		MessageHeaderAccessor mha = new MessageHeaderAccessor();
		mha.copyHeaders(headers);
		kryo.writeObject(output, mha.toMap());
	}

	@Override
	public MessageHeaders read(Kryo kryo, Input input, Class<MessageHeaders> type) {
		@SuppressWarnings("unchecked")
		Map<String, Object> map = kryo.readObject(input, HashMap.class);
		return new MessageHeaders(map);
	}
}
