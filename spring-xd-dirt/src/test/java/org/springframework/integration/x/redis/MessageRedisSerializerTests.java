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

import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.springframework.integration.Message;
import org.springframework.integration.message.GenericMessage;
import org.springframework.xd.test.redis.RedisAvailableRule;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Unit test of {@link MessageRedisSerializer}
 *
 * @author Jennifer Hickey
 */
public class MessageRedisSerializerTests {

	@Rule
	public RedisAvailableRule redisAvailableRule = new RedisAvailableRule();

	@Test
	public void testSerializeAndDeserialize() {
		MessageRedisSerializer serializer = new MessageRedisSerializer();
		Map<String, Object> headers = new HashMap<String, Object>();
		headers.put("header1", "foo");
		headers.put("header2", "bar");
		GenericMessage<String> message = new GenericMessage<String>("Hello", headers);
		byte[] rawMessage = serializer.serialize(message);
		Message convertedMsg = serializer.deserialize(rawMessage);
		assertEquals("Hello", convertedMsg.getPayload());
		assertEquals("foo", convertedMsg.getHeaders().get("header1"));
		assertEquals("bar", convertedMsg.getHeaders().get("header2"));
		assertNotNull(convertedMsg.getHeaders().get("id"));
		assertNotNull(convertedMsg.getHeaders().get("timestamp"));
	}

	@Test
	public void testDeserializeNull() {
		MessageRedisSerializer serializer = new MessageRedisSerializer();
		assertNull(serializer.deserialize(null));
	}

}
