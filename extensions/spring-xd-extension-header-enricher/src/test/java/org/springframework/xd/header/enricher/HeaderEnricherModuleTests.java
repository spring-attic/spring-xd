/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.xd.header.enricher;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericGroovyApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.messaging.support.MessageBuilder;

/**
 * @author David Turanski
 */

public class HeaderEnricherModuleTests {

	private MessageChannel input;

	private SubscribableChannel output;

	private ConfigurableApplicationContext initializeModuleContext(String headerValues, Boolean overwrite) {
		GenericGroovyApplicationContext applicationContext = new GenericGroovyApplicationContext();
		Properties properties = new Properties();
		properties.setProperty("headers", headerValues);
		properties.put("overwrite", overwrite);
		PropertiesPropertySource propertiesPropertySource = new PropertiesPropertySource("options", properties);
		applicationContext.getEnvironment().getPropertySources().addFirst(propertiesPropertySource);
		applicationContext.load(
				new FileSystemResource("../../modules/processor/header-enricher/config/header-enricher.groovy"));
		applicationContext.refresh();
		input = applicationContext.getBean("input", MessageChannel.class);
		output = applicationContext.getBean("output", SubscribableChannel.class);
		return applicationContext;
	}

	@Test
	public void testLiteralValues() {
		String headerValues = "{\"foo\":\"'this is a foo'\", \"bar\":\"'this is a bar'\"}";
		initializeModuleContext(headerValues, false);
		Message<String> message = new GenericMessage<String>("hello");

		final AtomicBoolean received = new AtomicBoolean();

		output.subscribe(new MessageHandler() {

			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				assertEquals("this is a foo", message.getHeaders().get("foo"));
				assertEquals("this is a bar", message.getHeaders().get("bar"));
				received.set(true);
			}
		});

		input.send(message);
		assertTrue(received.get());
	}

	@Test
	public void testSimpleExpressions() {
		String headerValues = "{\"foo\":\"(payload+', world!').toUpperCase()\",\"bar\":\"payload.substring(1)\"}";
		initializeModuleContext(headerValues, false);
		Message<String> message = new GenericMessage<String>("hello");

		final AtomicBoolean received = new AtomicBoolean();

		output.subscribe(new MessageHandler() {

			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				assertEquals("HELLO, WORLD!", message.getHeaders().get("foo"));
				assertEquals("ello", message.getHeaders().get("bar"));
				received.set(true);
			}
		});

		input.send(message);
		assertTrue(received.get());
	}

	@Test
	public void testHeaderOverwrite() {
		String headerValues = "{\"foo\":\"(payload+', world!').toUpperCase()\"}";
		initializeModuleContext(headerValues, true);
		Map<String, Object> messageHeaders = Collections.singletonMap("foo", (Object) "oldValue");
		Message<String> message = MessageBuilder.createMessage("hello", new MessageHeaders(messageHeaders));

		final AtomicBoolean received = new AtomicBoolean();
		output.subscribe(new MessageHandler() {

			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				assertEquals("HELLO, WORLD!", message.getHeaders().get("foo"));
				received.set(true);
			}
		});

		input.send(message);
		assertTrue(received.get());

	}
}
