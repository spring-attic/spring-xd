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
package org.springframework.integration.x.channel.registry;

import java.util.Collection;
import java.util.List;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.support.GenericApplicationContext;

/**
 * @author Gary Russell
 * @author David Turanski
 * @since 1.0
 *
 */
public class LocalChannelRegistryTests extends AbstractChannelRegistryTests {

	@Override
	protected ChannelRegistry getRegistry() throws Exception {
		LocalChannelRegistry registry = new LocalChannelRegistry();
		registry.setApplicationContext(new GenericApplicationContext());
		registry.afterPropertiesSet();
		return registry;
	}

	@Override
	protected Collection<?> getBridges(ChannelRegistry registry) {
		DirectFieldAccessor accessor = new DirectFieldAccessor(registry);
		List<?> bridges = (List<?>) accessor.getPropertyValue("bridges");
		return bridges;
	}

//	@Test
//	public void testPayloadConversionToString() throws Exception {
//		LocalChannelRegistry registry = (LocalChannelRegistry) getRegistry();
//		registry.setDefaultPayloadTransformer(new ObjectToStringTransformer());
//		verifyPayloadConversion("foo", registry);
//	}
//
//	@Test
//	public void testNoPayloadConversionByDefault() throws Exception {
//		LocalChannelRegistry registry = (LocalChannelRegistry) getRegistry();
//		verifyPayloadConversion(new Foo(), registry);
//	}
//
//	private void verifyPayloadConversion(final Object expectedValue, final LocalChannelRegistry registry) {
//		DirectChannel myChannel = new DirectChannel();
//		registry.inbound("in", myChannel);
//		DirectChannel input = registry.lookupSharedChannel("in", DirectChannel.class);
//		assertNotNull(input);
//
//		final AtomicBoolean msgSent = new AtomicBoolean(false);
//
//		myChannel.subscribe(new MessageHandler() {
//
//			@Override
//			public void handleMessage(Message<?> message) throws MessagingException {
//				assertEquals(expectedValue, message.getPayload());
//				msgSent.set(true);
//			}
//		});
//
//		Message<Foo> msg = MessageBuilder.withPayload(new Foo())
//				.setHeader(MessageHeaders.CONTENT_TYPE, MediaType.ALL_VALUE)
//				.build();
//
//		input.send(msg);
//		assertTrue(msgSent.get());
//	}

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
