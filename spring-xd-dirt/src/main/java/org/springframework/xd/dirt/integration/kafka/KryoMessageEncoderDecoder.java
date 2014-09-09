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

package org.springframework.xd.dirt.integration.kafka;

import java.util.HashMap;
import java.util.Map;

import kafka.serializer.Decoder;
import kafka.serializer.Encoder;
import kafka.utils.VerifiableProperties;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.messaging.support.MessageHeaderAccessor;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.shaded.org.objenesis.strategy.StdInstantiatorStrategy;


/**
 *
 * @author Eric Bottard
 */
public class KryoMessageEncoderDecoder implements Encoder<Message<?>>, Decoder<Message<?>> {


	public KryoMessageEncoderDecoder() {
		this(new VerifiableProperties());
	}

	public KryoMessageEncoderDecoder(VerifiableProperties properties) {
	}

	@Override
	public Message<?> fromBytes(byte[] bytes) {
		if (bytes == null) {
			return null;
		}
		Kryo kryo = kryoInstance();
		Input input = new Input(bytes);
		return kryo.readObjectOrNull(input, GenericMessage.class);
	}

	@Override
	public byte[] toBytes(Message<?> message) {
		Kryo kryo = kryoInstance();
		Output output = new Output(2048, -1);
		kryo.writeObjectOrNull(output, message, GenericMessage.class);
		return output.getBuffer();
	}

	private Kryo kryoInstance() {
		Kryo kryo = new Kryo();
		kryo.register(MessageHeaders.class, new KryoMessageHeadersSerializer());
		kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
		return kryo;
	}

	public static class KryoMessageHeadersSerializer extends Serializer<MessageHeaders> {

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


}
