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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.xd.tuple.DefaultTuple;
import org.springframework.xd.tuple.Tuple;
import org.springframework.xd.tuple.TupleBuilder;


/**
 * Tests for converting from a Message.
 *
 * @author David Turanski
 */
public class FromMessageConverterTests {

	private List<AbstractFromMessageConverter> converters = new ArrayList<AbstractFromMessageConverter>();

	CompositeMessageConverterFactory converterFactory;

	@Before
	public void setUp() {
		// Order matters
		converters.add(new StringToByteArrayMessageConverter());
		converters.add(new JavaToSerializedMessageConverter());
		converters.add(new SerializedToJavaMessageConverter());
		converters.add(new JsonToTupleMessageConverter());
		converters.add(new TupleToJsonMessageConverter());
		converters.add(new PojoToJsonMessageConverter());
		converters.add(new JsonToPojoMessageConverter());
		converters.add(new ByteArrayToStringMessageConverter());
		converters.add(new PojoToStringMessageConverter());
		converterFactory = new CompositeMessageConverterFactory(converters);
	}

	@Test
	public void testPojoToJsonPrettyPrint() {
		//OS agnostic
		String json = String.format("{%n  \"foo\" : \"bar\"%n}");
		PojoToJsonMessageConverter messageConverter = new PojoToJsonMessageConverter();
		messageConverter.setPrettyPrint(true);
		Message<?> msg = (Message<?>) messageConverter.fromMessage(new GenericMessage<Foo>(new Foo()), String.class);
		assertEquals(json, msg.getPayload().toString().trim());
		assertEquals(MimeTypeUtils.APPLICATION_JSON, msg.getHeaders().get(MessageHeaders.CONTENT_TYPE));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testTupleToJsonPrettyPrint() {
		Tuple t = TupleBuilder.fromString("{\"foo\":\"bar\"}");
		Message<?> msg = MessageBuilder.withPayload(t).build();
		TupleToJsonMessageConverter messageConverter = new TupleToJsonMessageConverter();
		messageConverter.setPrettyPrint(true);
		Message<String> result = (Message<String>) messageConverter.fromMessage(msg, String.class);
		assertTrue(result.getPayload(), result.getPayload().contains(String.format("{%n")));
		assertTrue(result.getPayload(), result.getPayload().contains("\"foo\" : \"bar\""));
		assertEquals(MimeTypeUtils.APPLICATION_JSON,
				result.getHeaders().get(MessageHeaders.CONTENT_TYPE));
	}

	@Test
	public void testPojoToJsonString() {
		String json = "{\"foo\":\"bar\"}";
		CompositeMessageConverter converter = converterFactory.newInstance(MimeTypeUtils.APPLICATION_JSON);
		Message<?> msg = (Message<?>) converter.fromMessage(new GenericMessage<Foo>(new Foo()), String.class);
		assertEquals(json, msg.getPayload());
		assertEquals(MimeTypeUtils.APPLICATION_JSON, msg.getHeaders().get(MessageHeaders.CONTENT_TYPE));
	}

	@Test
	public void testObjectToString() {
		String text = "[bar]";
		CompositeMessageConverter converter = converterFactory.newInstance(MimeTypeUtils.TEXT_PLAIN);
		Message<?> msg = (Message<?>) converter.fromMessage(new GenericMessage<Foo>(new Foo()), String.class);
		assertEquals(text, msg.getPayload());
		assertEquals(MimeTypeUtils.TEXT_PLAIN, msg.getHeaders().get(MessageHeaders.CONTENT_TYPE));
	}

	@Test
	public void testJsonStringToPojo() {
		String json = "{\"foo\":\"bar\"}";
		Message<?> msg = MessageBuilder.withPayload(json)
				.build();
		CompositeMessageConverter converter = converterFactory.newInstance(MimeType.valueOf("application/x-java-object"));
		Message<?> result = (Message<?>) converter.fromMessage(msg, Foo.class);
		Foo foo = (Foo) result.getPayload();
		assertEquals("bar", foo.getFoo());
		assertEquals(MimeType.valueOf("application/x-java-object;type=" + foo.getClass().getName()),
				result.getHeaders().get(MessageHeaders.CONTENT_TYPE));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testJsonStringToMap() {
		String json = "{\"foo\":\"bar\"}";
		Message<?> msg = MessageBuilder.withPayload(json)
				.build();
		CompositeMessageConverter converter = converterFactory.newInstance(MimeType.valueOf("application/x-java-object"));
		Message<Map<String, String>> result = (Message<Map<String, String>>) converter.fromMessage(msg, HashMap.class);
		Map<String, String> map = result.getPayload();
		assertEquals("bar", map.get("foo"));
		assertEquals(MimeType.valueOf("application/x-java-object;type=" + HashMap.class.getName()),
				result.getHeaders().get(MessageHeaders.CONTENT_TYPE));
	}

	@Test
	public void testJsonStringWithContentTypeHeaderToPojo() {
		String json = "{\"foo\":\"bar\"}";

		Message<?> msg = MessageBuilder.withPayload(json).copyHeaders(
				Collections.singletonMap(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON))
				.build();
		CompositeMessageConverter converter = converterFactory.newInstance(MimeType.valueOf("application/x-java-object"));

		Message<?> result = (Message<?>) converter.fromMessage(msg, Foo.class);
		Foo foo = (Foo) result.getPayload();
		assertEquals("bar", foo.getFoo());
		assertEquals(MimeType.valueOf("application/x-java-object;type=" + foo.getClass().getName()),
				result.getHeaders().get(MessageHeaders.CONTENT_TYPE));
	}

	@Test
	public void testJsonStringWithWrongContentTypeHeaderReturnsNull() {
		String json = "{\"foo\":\"bar\"}";
		CompositeMessageConverter converter = converterFactory.newInstance(MimeTypeUtils.APPLICATION_JSON);

		Message<?> msg = MessageBuilder.withPayload(json).copyHeaders(
				Collections.singletonMap(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_XML))
				.build();

		Message<?> result = (Message<?>) converter.fromMessage(msg, Foo.class);
		assertNull(result);
	}

	@Test
	public void testByteArrayToString() {
		String json = "{\"foo\":\"bar\"}";
		Message<?> msg = MessageBuilder.withPayload(json.getBytes()).build();
		CompositeMessageConverter converter = converterFactory.newInstance(MimeType.valueOf("text/plain"));

		Object result = converter.fromMessage(msg, String.class);
		assertEquals(json, result);
	}

	@Test
	public void testByteArrayToStringWithCharset() throws UnsupportedEncodingException {
		String text = "Hello \u3044\u3002";
		CompositeMessageConverter converter = converterFactory.newInstance(MimeType.valueOf("application/x-xd-string"));
		Message<?> msg = MessageBuilder.withPayload(text.getBytes("UTF-8")).copyHeaders(
				Collections.singletonMap(MessageHeaders.CONTENT_TYPE, MimeType.valueOf("text/plain;Charset=UTF-8")))
				.build();

		Object result = converter.fromMessage(msg, String.class);
		assertEquals(text, result);

		msg = MessageBuilder.withPayload(text.getBytes("ISO-8859-1")).copyHeaders(
				Collections.singletonMap(MessageHeaders.CONTENT_TYPE, MimeType.valueOf("text/plain;Charset=ISO-8859-1")))
				.build();
		result = converter.fromMessage(msg, String.class);
		assertEquals("Hello ??", result);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testJsonToTuple() {
		String json = "{\"foo\":\"bar\"}";
		Message<?> msg = MessageBuilder.withPayload(json).build();
		CompositeMessageConverter converter = converterFactory.newInstance(MimeType.valueOf("application/x-xd-tuple"));
		Message<Tuple> result = (Message<Tuple>) converter.fromMessage(msg, DefaultTuple.class);
		assertEquals("bar", result.getPayload().getString("foo"));
		assertEquals(MessageConverterUtils.javaObjectMimeType(DefaultTuple.class),
				result.getHeaders().get(MessageHeaders.CONTENT_TYPE));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testTupleToJson() {
		Tuple t = TupleBuilder.fromString("{\"foo\":\"bar\"}");
		Message<?> msg = MessageBuilder.withPayload(t).build();
		CompositeMessageConverter converter = converterFactory.newInstance(MimeTypeUtils.APPLICATION_JSON);

		Message<String> result = (Message<String>) converter.fromMessage(msg, String.class);
		assertTrue(result.getPayload(), result.getPayload().contains("\"foo\":\"bar\""));
		assertEquals(MimeTypeUtils.APPLICATION_JSON,
				result.getHeaders().get(MessageHeaders.CONTENT_TYPE));
	}

	@Test
	public void testJavaSerialization() {
		Foo foo = new Foo();
		CompositeMessageConverter converter = converterFactory.newInstance(MimeType.valueOf("application/x-java-serialized-object"));
		Message<Foo> msg = MessageBuilder.withPayload(foo).copyHeaders(
				Collections.singletonMap(MessageHeaders.CONTENT_TYPE, MimeType.valueOf("application/x-java-object"))).build();
		Message<?> result = (Message<?>) converter.fromMessage(msg, byte[].class);
		assertTrue(result.getPayload() instanceof byte[]);
		assertEquals(MimeType.valueOf("application/x-java-serialized-object"),
				result.getHeaders().get(MessageHeaders.CONTENT_TYPE));

		// Now convert back
		converter = converterFactory.newInstance(MimeType.valueOf("application/x-java-object"));
		result = (Message<?>) converter.fromMessage(result, Foo.class);
		assertNotNull(result);
		assertTrue(result.getPayload() instanceof Foo);
		assertEquals(MimeType.valueOf("application/x-java-object;type=" + Foo.class.getName()),
				result.getHeaders().get(MessageHeaders.CONTENT_TYPE));
	}

	@Test
	public void testStringToByteArray() throws UnsupportedEncodingException {
		CompositeMessageConverter converter = converterFactory.newInstance(MimeType.valueOf("application/octet-stream"));
		Message<String> msg = MessageBuilder.withPayload("hello").copyHeaders(
				Collections.singletonMap(MessageHeaders.CONTENT_TYPE, MimeType.valueOf("text/plain;charset=UTF-8"))).build();
		byte[] result = (byte[]) converter.fromMessage(msg, byte[].class);
		assertEquals("hello", new String(result, "UTF-8"));
	}
}


@SuppressWarnings("serial")
class Foo implements Serializable {

	private String foo = "bar";

	public String getFoo() {
		return foo;
	}

	@Override
	public String toString() {
		return "[" + foo + "]";
	}
}
