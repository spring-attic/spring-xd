/*
 * Copyright 2013 the original author or authors.
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
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.MessagingException;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.core.SubscribableChannel;
import org.springframework.integration.message.GenericMessage;
import org.springframework.xd.module.SimpleModule;
import org.springframework.xd.tuple.Tuple;

/**
 * @author David Turanski
 */
public class TypeConvertingStreamTests extends AbstractStreamTests {

	@BeforeClass
	public static void setup() {
		deployStream(
				"test1",
				"source --outputType=application/json | sink --inputType=application/x-xd-tuple");
		SubscribableChannel consumer = getSinkInputChannel("test1");
		consumer.subscribe(new MessageHandler() {

			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
			}
		});
	}

	@Test
	public void testParametersPresent() {
		SimpleModule source = getDeployedSource("test1");
		SimpleModule sink = getDeployedSink("test1");
		assertEquals("application/json", source.getProperties().get("outputType"));
		assertEquals("application/x-xd-tuple", sink.getProperties().get("inputType"));
	}

	@Test
	public void testBasicTypeConversion() {

		MessageChannel producer = getSourceOutputChannel("test1");
		SubscribableChannel consumer = getSinkInputChannel("test1");

		MessageTest test = new MessageTest() {

			@Override
			public void test(Message<?> message) throws MessagingException {
				assertTrue(message.getPayload() instanceof Tuple);
				Tuple t = (Tuple) message.getPayload();
				assertEquals("bar", t.getString("s"));
				assertEquals(9999, t.getInt("i"));
			}
		};
		consumer.subscribe(test);
		producer.send(new GenericMessage<Foo>(new Foo("bar", 9999)));
		assertTrue(test.getMessageHandled());

		sendPayloadAndVerifyOutput("test1", new Foo("bar", 9999), test);
	}

	@Test
	public void testBasicTypeConversionWithTap() {

		MessageTest test = new MessageTest() {

			@Override
			public void test(Message<?> message) throws MessagingException {
				assertTrue(message.getPayload() instanceof String);
				assertEquals("{\"s\":\"bar\",\"i\":9999}", message.getPayload());
			}
		};

		sendPayloadAndVerifyTappedOutput("test1", new Foo("bar", 9999), "source", test);

	}

	/**
	 * 
	 */
	static class Foo {

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
	}
}
