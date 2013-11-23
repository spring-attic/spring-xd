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

package org.springframework.integration.x.redis;

import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.GenericMessage;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.shaded.org.objenesis.strategy.StdInstantiatorStrategy;


/**
 * A Redis Serializer that uses Kryo serialization
 * 
 * @author David Turanski
 * @since 1.0
 */
public class KryoMessageSerializer implements RedisSerializer<Message<?>> {

	@Override
	public byte[] serialize(Message<?> message) throws SerializationException {
		Kryo kryo = kryoInstance();
		Output output = new Output(2048, -1);
		kryo.writeObjectOrNull(output, message, GenericMessage.class);
		return output.getBuffer();
	}

	@Override
	public Message<?> deserialize(byte[] bytes) throws SerializationException {
		if (bytes == null) {
			return null;
		}
		Kryo kryo = kryoInstance();
		Input input = new Input(bytes);
		return kryo.readObjectOrNull(input, GenericMessage.class);
	}

	private Kryo kryoInstance() {
		Kryo kryo = new Kryo();
		kryo.register(MessageHeaders.class, new KryoMessageHeadersSerializer());
		kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
		return kryo;
	}

}
