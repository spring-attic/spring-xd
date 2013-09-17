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

package org.springframework.integration.x.bus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import org.junit.Test;

import org.springframework.http.MediaType;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.xd.tuple.DefaultTuple;
import org.springframework.xd.tuple.Tuple;
import org.springframework.xd.tuple.TupleBuilder;

/**
 * @author Gary Russell
 * @author David Turanski
 */
public class MessageBusSupportTests {

	private final TestMessageBus messageBus = new TestMessageBus();

	@Test
	public void testBytesPassThru() {
		byte[] payload = "foo".getBytes();
		Message<byte[]> message = MessageBuilder.withPayload(payload).build();
		Message<?> converted = messageBus.transformOutboundIfNecessary(message,
				MediaType.APPLICATION_OCTET_STREAM);
		assertSame(payload, converted.getPayload());
		assertEquals(MediaType.APPLICATION_OCTET_STREAM,
				converted.getHeaders().get(MessageHeaders.CONTENT_TYPE));
		Message<?> reconstructed = messageBus.transformInboundIfNecessary(converted,
				Collections.singletonList(MediaType.ALL));
		payload = (byte[]) reconstructed.getPayload();
		assertSame(converted.getPayload(), payload);
		assertNull(reconstructed.getHeaders().get(MessageBusSupport.ORIGINAL_CONTENT_TYPE_HEADER));
	}

	@Test
	public void testBytesPassThruContentType() {
		byte[] payload = "foo".getBytes();
		Message<byte[]> message = MessageBuilder.withPayload(payload)
				.setHeader(MessageHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
				.build();
		Message<?> converted = messageBus.transformOutboundIfNecessary(message,
				MediaType.APPLICATION_OCTET_STREAM);
		assertSame(payload, converted.getPayload());
		assertEquals(MediaType.APPLICATION_OCTET_STREAM,
				converted.getHeaders().get(MessageHeaders.CONTENT_TYPE));
		Message<?> reconstructed = messageBus.transformInboundIfNecessary(converted,
				Collections.singletonList(MediaType.ALL));
		payload = (byte[]) reconstructed.getPayload();
		assertSame(converted.getPayload(), payload);
		assertEquals(MediaType.APPLICATION_OCTET_STREAM_VALUE,
				reconstructed.getHeaders().get(MessageHeaders.CONTENT_TYPE));
		assertNull(reconstructed.getHeaders().get(MessageBusSupport.ORIGINAL_CONTENT_TYPE_HEADER));
	}

	@Test
	public void testString() throws IOException {
		Message<?> converted = messageBus.transformOutboundIfNecessary(
				new GenericMessage<String>("foo"), MediaType.APPLICATION_OCTET_STREAM);

		assertEquals(MediaType.TEXT_PLAIN,
				converted.getHeaders().get(MessageHeaders.CONTENT_TYPE));
		Message<?> reconstructed = messageBus.transformInboundIfNecessary(converted,
				Collections.singletonList(MediaType.ALL));
		assertEquals("foo", reconstructed.getPayload());
		assertNull(reconstructed.getHeaders().get(MessageHeaders.CONTENT_TYPE));
	}

	@Test
	public void testContentTypePreserved() throws IOException {
		Message<String> inbound = MessageBuilder.withPayload("{\"foo\":\"foo\"}")
				.copyHeaders(Collections.singletonMap(MessageHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON))
				.build();
		Message<?> converted = messageBus.transformOutboundIfNecessary(
				inbound, MediaType.APPLICATION_OCTET_STREAM);

		assertEquals(MediaType.TEXT_PLAIN,
				converted.getHeaders().get(MessageHeaders.CONTENT_TYPE));
		assertEquals(MediaType.APPLICATION_JSON,
				converted.getHeaders().get(MessageBusSupport.ORIGINAL_CONTENT_TYPE_HEADER));
		Message<?> reconstructed = messageBus.transformInboundIfNecessary(converted,
				Collections.singletonList(MediaType.ALL));
		assertEquals("{\"foo\":\"foo\"}", reconstructed.getPayload());
		assertEquals(MediaType.APPLICATION_JSON, reconstructed.getHeaders().get(MessageHeaders.CONTENT_TYPE));
	}

	@Test
	public void testPojoSerialization() {
		Message<?> converted = messageBus.transformOutboundIfNecessary(new GenericMessage<Foo>(new Foo("bar")),
				MediaType.APPLICATION_OCTET_STREAM);
		MediaType mediaType = (MediaType) converted.getHeaders().get(MessageHeaders.CONTENT_TYPE);
		assertEquals("application", mediaType.getType());
		assertEquals("x-java-object", mediaType.getSubtype());
		assertEquals(Foo.class.getName(), mediaType.getParameter("type"));

		Message<?> reconstructed = messageBus.transformInboundIfNecessary(converted,
				Collections.singletonList(MediaType.ALL));
		assertEquals("bar", ((Foo) reconstructed.getPayload()).getBar());
		assertNull(reconstructed.getHeaders().get(MessageHeaders.CONTENT_TYPE));
	}

	@Test
	public void testPojoWithXJavaObjectMediaTypeNoType() {
		Message<?> converted = messageBus.transformOutboundIfNecessary(new GenericMessage<Foo>(new Foo("bar")),
				MediaType.APPLICATION_OCTET_STREAM);
		MediaType mediaType = (MediaType) converted.getHeaders().get(MessageHeaders.CONTENT_TYPE);
		assertEquals("application", mediaType.getType());
		assertEquals("x-java-object", mediaType.getSubtype());
		assertEquals(Foo.class.getName(), mediaType.getParameter("type"));

		Message<?> reconstructed = messageBus.transformInboundIfNecessary(converted,
				Collections.singletonList(new MediaType("application", "x-java-object")));
		assertEquals("bar", ((Foo) reconstructed.getPayload()).getBar());
		assertNull(reconstructed.getHeaders().get(MessageHeaders.CONTENT_TYPE));
	}

	@Test
	public void testPojoWithXJavaObjectMediaTypeExplicitType() {
		Message<?> converted = messageBus.transformOutboundIfNecessary(new GenericMessage<Foo>(new Foo("bar")),
				MediaType.APPLICATION_OCTET_STREAM);
		MediaType mediaType = (MediaType) converted.getHeaders().get(MessageHeaders.CONTENT_TYPE);
		assertEquals("application", mediaType.getType());
		assertEquals("x-java-object", mediaType.getSubtype());
		assertEquals(Foo.class.getName(), mediaType.getParameter("type"));

		Message<?> reconstructed = messageBus.transformInboundIfNecessary(converted,
				Collections.singletonList(mediaType));
		assertEquals("bar", ((Foo) reconstructed.getPayload()).getBar());
		assertNull(reconstructed.getHeaders().get(MessageHeaders.CONTENT_TYPE));
	}

	@Test
	public void testTupleSerialization() {
		Tuple payload = TupleBuilder.tuple().of("foo", "bar");
		Message<?> converted = messageBus.transformOutboundIfNecessary(new GenericMessage<Tuple>(payload),
				MediaType.APPLICATION_OCTET_STREAM);
		MediaType mediaType = (MediaType) converted.getHeaders().get(MessageHeaders.CONTENT_TYPE);
		assertEquals("application", mediaType.getType());
		assertEquals("x-java-object", mediaType.getSubtype());
		assertEquals(DefaultTuple.class.getName(), mediaType.getParameter("type"));

		Message<?> reconstructed = messageBus.transformInboundIfNecessary(converted,
				Collections.singletonList(MediaType.ALL));
		assertEquals("bar", ((Tuple) reconstructed.getPayload()).getString("foo"));
		assertNull(reconstructed.getHeaders().get(MessageHeaders.CONTENT_TYPE));
	}

	public static class Foo {

		private String bar;

		public Foo() {
		}

		public Foo(String bar) {
			this.bar = bar;
		}

		public String getBar() {
			return bar;
		}

		public void setBar(String bar) {
			this.bar = bar;
		}

	}

	public static class Bar {

		private String foo;

		public Bar() {
		}

		public Bar(String foo) {
			this.foo = foo;
		}

		public String getFoo() {
			return foo;
		}

		public void setFoo(String foo) {
			this.foo = foo;
		}

	}

	public class TestMessageBus extends MessageBusSupport {

		@Override
		public void bindConsumer(String name, MessageChannel channel, Collection<MediaType> acceptedMediaTypes,
				boolean aliasHint) {
		}

		@Override
		public void bindPubSubConsumer(String name, MessageChannel moduleInputChannel,
				Collection<MediaType> acceptedMediaTypes) {
		}

		@Override
		public void bindPubSubProducer(String name, MessageChannel moduleOutputChannel) {
		}

		@Override
		public void bindProducer(String name, MessageChannel channel, boolean aliasHint) {
		}
	}

}
