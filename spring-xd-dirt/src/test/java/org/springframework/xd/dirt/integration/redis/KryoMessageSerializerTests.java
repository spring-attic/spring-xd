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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.xd.dirt.integration.redis.KryoMessageSerializer;


/**
 * 
 * @author David Turanski
 */
public class KryoMessageSerializerTests {

	private KryoMessageSerializer serializer = new KryoMessageSerializer();

	@Test
	public void testSerializeAndDeserialize() {
		Map<String, Object> headers = new HashMap<String, Object>();
		headers.put("header1", "foo");
		headers.put("header2", "bar");
		Foo foo = new Foo(123, "hello");
		headers.put("header3", foo);
		GenericMessage<String> message = new GenericMessage<String>("Hello", headers);
		byte[] rawMessage = serializer.serialize(message);
		@SuppressWarnings("unchecked")
		Message<String> convertedMsg = (Message<String>) serializer.deserialize(rawMessage);
		assertEquals("Hello", convertedMsg.getPayload());
		assertEquals("foo", convertedMsg.getHeaders().get("header1"));
		assertEquals("bar", convertedMsg.getHeaders().get("header2"));
		assertNotNull(convertedMsg.getHeaders().get("id"));
		assertNotNull(convertedMsg.getHeaders().get("timestamp"));
		assertEquals(foo, convertedMsg.getHeaders().get("header3"));
	}

	@Test
	public void testDeserializeNull() {
		assertNull(serializer.deserialize(null));
	}

	public static class Foo {

		private final int i;

		private final String s;

		public Foo(int i, String s) {
			this.i = i;
			this.s = s;
		}

		public int getI() {
			return i;
		}

		public String getS() {
			return s;
		}

		@Override
		public boolean equals(Object other) {
			if (!(other instanceof Foo)) {
				return false;
			}
			Foo foo = (Foo) other;

			return i == foo.i && (s == null && foo.s == null || s.equals(foo.s));
		}

	}

}
