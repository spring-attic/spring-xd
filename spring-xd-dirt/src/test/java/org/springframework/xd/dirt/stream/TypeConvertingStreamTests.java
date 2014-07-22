/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.xd.dirt.stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.converter.ContentTypeResolver;
import org.springframework.util.MimeType;
import org.springframework.xd.dirt.integration.bus.StringConvertingContentTypeResolver;
import org.springframework.xd.dirt.integration.bus.converter.MessageConverterUtils;
import org.springframework.xd.module.core.Module;
import org.springframework.xd.tuple.DefaultTuple;
import org.springframework.xd.tuple.Tuple;

/**
 * @author David Turanski
 */
public class TypeConvertingStreamTests extends StreamTestSupport {

	private ContentTypeResolver contentTypeResolver = new StringConvertingContentTypeResolver();

	@BeforeClass
	public static void setup() {
		deployStream(
				"test1",
				"source --outputType=application/json | sink --inputType=application/x-xd-tuple");
	}

	@Test
	public void testParametersPresent() {
		Module source = getDeployedSource("test1");
		Module sink = getDeployedSink("test1");
		assertEquals("application/json", source.getProperties().get("outputType"));
		assertEquals("application/x-xd-tuple", sink.getProperties().get("inputType"));
	}

	@Test
	public void testBasicTypeConversion() {

		MessageTest test = new MessageTest() {

			@Override
			public void test(Message<?> message) throws MessagingException {
				assertTrue(message.getPayload() instanceof Tuple);
				Tuple t = (Tuple) message.getPayload();
				assertEquals("bar", t.getString("s"));
				assertEquals(9999, t.getInt("i"));
				assertEquals(MessageConverterUtils.javaObjectMimeType(DefaultTuple.class),
						contentTypeResolver.resolve(message.getHeaders()));
			}
		};

		sendPayloadAndVerifyOutput("test1", new Foo("bar", 9999), test);
	}

	@Test
	public void testBasicTypeConversionWithTap() {

		MessageTest test = new MessageTest() {

			@Override
			public void test(Message<?> message) throws MessagingException {
				assertTrue(message.getPayload() instanceof String);
				assertEquals("{\"s\":\"bar\",\"i\":9999}", message.getPayload());
				// TODO: This not working with the tap
				// assertEquals(MediaType.APPLICATION_JSON,
				// message.getHeaders().get(MessageHeaders.CONTENT_TYPE));
			}
		};

		sendPayloadAndVerifyTappedOutput("test1", new Foo("bar", 9999), "source", test);

	}

	@Test
	public void testInputTypeAsTJavaTypeConversion() {

		deployStream(
				"test2",
				"source --outputType=application/json | sink --inputType=" + Tuple.class.getName());

		MessageTest test = new MessageTest() {

			@Override
			public void test(Message<?> message) throws MessagingException {
				assertTrue(message.getPayload() instanceof Tuple);
				Tuple t = (Tuple) message.getPayload();
				assertEquals("bar", t.getString("s"));
				assertEquals(9999, t.getInt("i"));
				assertEquals(MimeType.valueOf("application/x-java-object;type=" + DefaultTuple.class.getName()),
						contentTypeResolver.resolve(message.getHeaders()));
			}
		};

		sendPayloadAndVerifyOutput("test2", new Foo("bar", 9999), test);
	}

	@Test
	public void testRawBytes() {
		deployStream(
				"rawbytes",
				"source --outputType=application/x-java-serialized-object | sink");

		MessageTest test = new MessageTest() {

			@Override
			public void test(Message<?> message) throws MessagingException {
				assertTrue(message.getPayload() instanceof byte[]);
				assertEquals(MimeType.valueOf("application/x-java-serialized-object"),
						contentTypeResolver.resolve(message.getHeaders()));
			}
		};
		sendPayloadAndVerifyOutput("rawbytes", new Foo("bar", 9999), test);
	}

	@Test
	public void unknownContentTypeWillNotDeploy() {
		assertFalse(deployStream(
				"xml",
				"source --outputType=application/xml | sink"));
	}

	@Test
	public void testObjectToStringConversion() {
		final Foo foo = new Foo("hello", 123);

		deployStream(
				"fooToString",
				"source --outputType=text/plain | sink");
		MessageTest test = new MessageTest() {

			@Override
			public void test(Message<?> message) throws MessagingException {
				assertEquals(foo.toString(), message.getPayload());
				assertEquals(MimeType.valueOf("text/plain"),
						contentTypeResolver.resolve(message.getHeaders()));
			}
		};
		sendPayloadAndVerifyOutput("fooToString", foo, test);
	}


	@Test
	public void testJsonToMapConversion() {

		deployStream(
				"jsonToMap",
				"source --outputType=application/json | sink --inputType=java.util.Map");
		MessageTest test = new MessageTest() {

			@SuppressWarnings("rawtypes")
			@Override
			public void test(Message<?> message) throws MessagingException {
				Map map = (Map) message.getPayload();
				assertEquals("hello", map.get("s"));
				assertEquals(123, map.get("i"));
				assertEquals(MimeType.valueOf("application/x-java-object;type=java.util.Map"),
						contentTypeResolver.resolve(message.getHeaders()));
			}
		};
		sendPayloadAndVerifyOutput("jsonToMap", new Foo("hello", 123), test);
	}

	@Test
	public void testDefaultTypeConversion() {
		final Date now = new Date();
		/*
		 * Should fall back to Default conversion service
		 */
		deployStream(
				"dateToString",
				"source --outputType=text/plain | sink");
		MessageTest test = new MessageTest() {

			@Override
			public void test(Message<?> message) throws MessagingException {
				assertEquals(now.toString(), message.getPayload());
				assertEquals(MimeType.valueOf("text/plain"),
						contentTypeResolver.resolve(message.getHeaders()));
			}
		};
		sendPayloadAndVerifyOutput("dateToString", now, test);
	}

	// TODO: Test with class value (testtupleprocessor) - fails currently because converted to string 'class
	// <className>'
	@Test
	public void testModuleWithDefaultInputTypeProperties() {
		deployStream(
				"defaultInputType",
				"source --outputType=application/json | testtupleprocessor | sink");
		MessageTest test = new MessageTest() {

			@Override
			public void test(Message<?> message) throws MessagingException {
				assertTrue(Tuple.class.isAssignableFrom(message.getPayload().getClass()));
				assertEquals(MimeType.valueOf("application/x-java-object;type=" + DefaultTuple.class.getName()),
						contentTypeResolver.resolve(message.getHeaders()));
			}
		};
		sendPayloadAndVerifyOutput("defaultInputType", new Foo("hello", 123), test);
	}

	@Test
	public void testOverrideInputTypeForModuleWithDefaultInputTypeProperties() {
		deployStream(
				"overrideInputType",
				"source --outputType=application/json | testtupleprocessor --inputType=java.lang.String | sink");
		MessageTest test = new MessageTest() {

			@Override
			public void test(Message<?> message) throws MessagingException {
				assertTrue(message.getPayload() instanceof String);
			}
		};
		sendPayloadAndVerifyOutput("overrideInputType", new Foo("hello", 123), test);
	}

	@Test
	public void testModuleWithDefaultInputTypeDefinedAsClass() {
		deployStream(
				"classInputType",
				"source --outputType=application/json | testtupleprocessor_pojo_config | sink");
		MessageTest test = new MessageTest() {

			@Override
			public void test(Message<?> message) throws MessagingException {
				assertTrue(Tuple.class.isAssignableFrom(message.getPayload().getClass()));
				assertEquals(MimeType.valueOf("application/x-java-object;type=" + DefaultTuple.class.getName()),
						contentTypeResolver.resolve(message.getHeaders()));
			}
		};

		sendPayloadAndVerifyOutput("classInputType", new Foo("hello", 123), test);
	}

	@Test
	public void testStringToByteArray() {
		deployStream(
				"stringToBytes",
				"source --outputType=application/octet-stream | sink");
		MessageTest test = new MessageTest() {

			@Override
			public void test(Message<?> message) throws MessagingException {
				assertTrue(byte[].class.isAssignableFrom(message.getPayload().getClass()));
				assertEquals(MimeType.valueOf("text/plain;charset=UTF-8"),
						contentTypeResolver.resolve(message.getHeaders()));
				try {
					assertEquals("hello\u00F6\u00FF", new String((byte[]) message.getPayload(), "UTF-8"));
				}
				catch (UnsupportedEncodingException e) {
				}
			}
		};

		Message<String> msg = MessageBuilder.withPayload("hello\u00F6\u00FF").
				copyHeaders(Collections.singletonMap(MessageHeaders.CONTENT_TYPE, "text/plain;charset=UTF-8")).build();
		sendMessageAndVerifyOutput("stringToBytes", msg, test);
	}

	@SuppressWarnings("serial")
	static class Foo implements Serializable {

		private String s;

		private int i;

		public Foo(String s, int i) {
			this.s = s;
			this.i = i;
		}

		public String getS() {
			return s;
		}

		public void setS(String s) {
			this.s = s;
		}

		public int getI() {
			return i;
		}

		public void setI(int i) {
			this.i = i;
		}

		@Override
		public String toString() {
			return s + ":" + i;
		}
	}
}
