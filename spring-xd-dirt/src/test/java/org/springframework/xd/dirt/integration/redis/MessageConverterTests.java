/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.xd.dirt.integration.redis;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.xd.dirt.integration.bus.EmbeddedHeadersMessageConverter;

/**
 * @author Gary Russell
 * @since 1.0
 * 
 */
public class MessageConverterTests {

	@Test
	public void testHeaderEmbedding() throws Exception {
		EmbeddedHeadersMessageConverter converter = new EmbeddedHeadersMessageConverter();
		Message<byte[]> message = MessageBuilder.withPayload("Hello".getBytes())
				.setHeader("foo", "bar")
				.setHeader("baz", "quxx")
				.build();
		Message<byte[]> converted = converter.embedHeaders(message, "foo", "baz");
		assertEquals("\u0002\u0003foo\u0003bar\u0003baz\u0004quxxHello", new String(converted.getPayload()));

		converted = converter.extractHeaders(converted);
		assertEquals("Hello", new String(converted.getPayload()));
		assertEquals("bar", converted.getHeaders().get("foo"));
		assertEquals("quxx", converted.getHeaders().get("baz"));
	}

	@Test
	public void testHeaderEmbeddingMissingHeader() throws Exception {
		EmbeddedHeadersMessageConverter converter = new EmbeddedHeadersMessageConverter();
		Message<byte[]> message = MessageBuilder.withPayload("Hello".getBytes())
				.setHeader("foo", "bar")
				.build();
		Message<byte[]> converted = converter.embedHeaders(message, "foo", "baz");
		assertEquals("\u0001\u0003foo\u0003barHello", new String(converted.getPayload()));
	}

}
