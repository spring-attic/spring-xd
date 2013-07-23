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
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;

import org.junit.Test;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.http.MediaType;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.message.GenericMessage;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.MethodCallback;
import org.springframework.xd.module.Module;
import org.springframework.xd.tuple.Tuple;
import org.springframework.xd.tuple.TupleBuilder;

/**
 * @author Gary Russell
 *
 */
public class ChannelRegistrySupportTests {

	private final TestChannelRegistry channelRegistry = new TestChannelRegistry();

	@Test
	public void testBytesPassThru() {
		byte[] payload = "foo".getBytes();
		Object converted = channelRegistry.transformPayloadFromOutputChannel(payload, MediaType.APPLICATION_OCTET_STREAM);
		assertEquals(payload, converted);
		payload = (byte[]) channelRegistry.transformPayloadFporInputChannel(converted, Collections.singletonList(MediaType.ALL));
		assertEquals(converted, payload);
	}

	@Test
	public void testJsonString() {
		Object converted = channelRegistry.transformPayloadFromOutputChannel("foo", MediaType.APPLICATION_OCTET_STREAM);
		assertEquals("{\"String\":\"foo\"}", new String((byte[]) converted));
		Object payload = channelRegistry.transformPayloadFporInputChannel(converted, Collections.singletonList(MediaType.ALL));
		assertEquals("foo", payload);
	}

	@Test
	public void testJsonPojo() {
		Object converted = channelRegistry.transformPayloadFromOutputChannel(new Foo("bar"), MediaType.APPLICATION_OCTET_STREAM);
		assertEquals("{\"Foo\":{\"@class\":\"org.springframework.integration.x.channel.registry.ChannelRegistrySupportTests$Foo\",\"bar\":\"bar\"}}", new String((byte[]) converted));
		Foo payload = (Foo) channelRegistry.transformPayloadFporInputChannel(converted, Collections.singletonList(MediaType.ALL));
		assertEquals("bar", payload.getBar());
	}

	@Test
	public void testJsonTuple() {
		Tuple payload = TupleBuilder.tuple().of("foo", "bar");
		Object converted = channelRegistry.transformPayloadFromOutputChannel(payload, MediaType.APPLICATION_OCTET_STREAM);
		payload = (Tuple) channelRegistry.transformPayloadFporInputChannel(converted, Collections.singletonList(MediaType.ALL));
		assertEquals("bar", payload.getString("foo"));
	}

	/*
	 * Foo transported as JSON, decoded and then converted to Bar
	 */
	@Test
	public void testJsonPojoConvert() {
		DefaultConversionService conversionService = new DefaultConversionService();
		conversionService.addConverter(new Converter<Foo, Bar>(){

			@Override
			public Bar convert(Foo source) {
				return new Bar(source.getBar());
			}
		});
		channelRegistry.setConversionService(conversionService);

		Object converted = channelRegistry.transformPayloadFromOutputChannel(new Foo("bar"), MediaType.APPLICATION_OCTET_STREAM);
		assertEquals("{\"Foo\":{\"@class\":\"org.springframework.integration.x.channel.registry.ChannelRegistrySupportTests$Foo\",\"bar\":\"bar\"}}", new String((byte[]) converted));

		MediaType type = new MediaType("application", "x-java-object", Collections.singletonMap("type",
				"org.springframework.integration.x.channel.registry.ChannelRegistrySupportTests$Bar"));
		Bar payload = (Bar) channelRegistry.transformPayloadFporInputChannel(converted, Collections.singletonList(type));
		assertEquals("bar", payload.getFoo());
	}

	/*
	 * Foo transported as JSON, decoded and then converted to Bar using higher level protected methods
	 */
	@Test
	public void testJsonPojoConvertMessage() {
		DefaultConversionService conversionService = new DefaultConversionService();
		conversionService.addConverter(new Converter<Foo, Bar>(){

			@Override
			public Bar convert(Foo source) {
				return new Bar(source.getBar());
			}
		});
		channelRegistry.setConversionService(conversionService);

		Message<?> message = new GenericMessage<Foo>(new Foo("bar"));
		Message<?> messageToSend = channelRegistry.transformOutboundIfNecessary(message, MediaType.APPLICATION_OCTET_STREAM);
		assertEquals("{\"Foo\":{\"@class\":\"org.springframework.integration.x.channel.registry.ChannelRegistrySupportTests$Foo\",\"bar\":\"bar\"}}",
				new String((byte[]) messageToSend.getPayload()));

		MediaType type = new MediaType("application", "x-java-object", Collections.singletonMap("type",
				"org.springframework.integration.x.channel.registry.ChannelRegistrySupportTests$Bar"));
		Module module = mock(Module.class);
		when(module.getAcceptedMediaTypes()).thenReturn(Collections.singletonList(type));
		Message<?> messageToSink = channelRegistry.transformInboundIfNecessary(messageToSend, module);
		assertEquals("bar", ((Bar) messageToSink.getPayload()).getFoo());
	}

	public static class Foo {

		private String bar;

		public Foo() {}

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

		public Bar() {}

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

	public class TestChannelRegistry extends ChannelRegistrySupport {

		private Method transformPayloadFromOutputChannel;

		private Method transformPayloadForInputChannel;

		public TestChannelRegistry() {
			ReflectionUtils.doWithMethods(ChannelRegistrySupport.class, new MethodCallback() {

				@Override
				public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
					if (method.getName().equals("transformPayloadFromOutputChannel")) {
						transformPayloadFromOutputChannel = method;
						method.setAccessible(true);
					}
					else if (method.getName().equals("transformPayloadForInputChannel")) {
						transformPayloadForInputChannel = method;
						method.setAccessible(true);
					}

				}
			});
		}

		@Override
		public void tap(String tapModule, String name, MessageChannel channel) {
		}

		@Override
		public void outbound(String name, MessageChannel channel, Module module) {
		}

		@Override
		public void inbound(String name, MessageChannel channel, Module module) {
		}

		@Override
		public void cleanAll(String name) {
		}

		public Object transformPayloadFromOutputChannel(Object payload, MediaType to) {
			try {
				return this.transformPayloadFromOutputChannel.invoke(this, payload, to);
			}
			catch (Exception e) {
				fail(e.getMessage());
			}
			return null;
		}

		public Object transformPayloadFporInputChannel(Object payload, Collection<MediaType> to) {
			try {
				return this.transformPayloadForInputChannel.invoke(this, payload, to);
			}
			catch (Exception e) {
				fail(e.getMessage());
			}
			return null;
		}

	}

}
