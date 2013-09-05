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

package org.springframework.integration.x.channel.registry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.http.MediaType;
import org.springframework.integration.Message;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.MessagingException;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.support.MessageBuilder;

/**
 * @author Gary Russell
 * @author David Turanski
 * @since 1.0
 * 
 */
public class LocalChannelRegistryTests extends AbstractChannelRegistryTests {

	private static final Collection<MediaType> ALL = Collections.singletonList(MediaType.ALL);

	@Override
	protected ChannelRegistry getRegistry() throws Exception {
		LocalChannelRegistry registry = new LocalChannelRegistry();
		registry.setApplicationContext(new GenericApplicationContext());
		registry.afterPropertiesSet();
		return registry;
	}

	@Test
	public void testPayloadConversionToString() throws Exception {
		LocalChannelRegistry registry = (LocalChannelRegistry) getRegistry();
		DefaultConversionService conversionService = new DefaultConversionService();
		conversionService.addConverter(new Converter<Foo, String>() {

			@Override
			public String convert(Foo source) {
				return source.toString();
			}
		});
		registry.setConversionService(conversionService);
		verifyPayloadConversion("foo", registry, Collections.singletonList(MediaType.TEXT_PLAIN));
	}

	@Test
	public void testPayloadConversionNotNeededExplicitType() throws Exception {
		LocalChannelRegistry registry = (LocalChannelRegistry) getRegistry();
		verifyPayloadConversion(new Foo(), registry, Collections.singletonList(new MediaType("application",
				"x-java-object", Collections.singletonMap("type",
						"org.springframework.integration.x.channel.registry.LocalChannelRegistryTests$Foo"))));
	}

	@Test
	public void testNoPayloadConversionByDefault() throws Exception {
		LocalChannelRegistry registry = (LocalChannelRegistry) getRegistry();
		verifyPayloadConversion(new Foo(), registry);
	}

	private void verifyPayloadConversion(final Object expectedValue, final LocalChannelRegistry registry) {
		verifyPayloadConversion(expectedValue, registry, ALL);
	}

	private void verifyPayloadConversion(final Object expectedValue, final LocalChannelRegistry registry,
			Collection<MediaType> acceptedMediaTypes) {
		DirectChannel myChannel = new DirectChannel();
		registry.createInbound("in", myChannel, acceptedMediaTypes, false);
		DirectChannel input = registry.getBean("in", DirectChannel.class);
		assertNotNull(input);

		final AtomicBoolean msgSent = new AtomicBoolean(false);

		myChannel.subscribe(new MessageHandler() {

			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				assertEquals(expectedValue, message.getPayload());
				msgSent.set(true);
			}
		});

		Message<Foo> msg = MessageBuilder.withPayload(new Foo())
				.setHeader(MessageHeaders.CONTENT_TYPE, MediaType.ALL_VALUE).build();

		input.send(msg);
		assertTrue(msgSent.get());
	}

	static class Foo {

		@Override
		public String toString() {
			return "foo";
		}

		@Override
		public boolean equals(Object other) {
			if (!(other instanceof Foo)) {
				return false;
			}
			return this.toString().equals(other.toString());
		}
	}
}
