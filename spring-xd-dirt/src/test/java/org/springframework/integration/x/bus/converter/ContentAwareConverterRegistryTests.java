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

package org.springframework.integration.x.bus.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import org.junit.Test;

import org.springframework.core.convert.converter.Converter;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.http.MediaType;
import org.springframework.xd.tuple.Tuple;

/**
 * 
 * @author David Turanski
 */
public class ContentAwareConverterRegistryTests {

	@Test
	public void test() {
		DefaultContentTypeAwareConverterRegistry converterRegistry = new DefaultContentTypeAwareConverterRegistry();
		assertNotNull(converterRegistry.getConverters(APPLICATION_JSON));
		Converter<?, ?> converter = converterRegistry.getConverters(APPLICATION_JSON).get(Object.class);
		assertTrue(converter instanceof MappingJackson2Converter);

		converter = converterRegistry.getConverters(APPLICATION_JSON).get(Tuple.class);
		assertNotNull(converter);
		assertTrue(converter instanceof TupleToJsonConverter);

		Class<?> javaType = converterRegistry.getJavaTypeForContentType(ContentTypeAwareConverterRegistry.X_XD_TUPLE,
				this.getClass().getClassLoader());
		assertTrue(Tuple.class.isAssignableFrom(javaType));

		javaType = converterRegistry.getJavaTypeForContentType(
				ContentTypeAwareConverterRegistry.X_JAVA_SERIALIZED_OBJECT, this.getClass().getClassLoader());
		assertTrue(byte[].class.equals(javaType));
	}

	@Test
	public void testCustomConverters() {
		DefaultFormattingConversionService conversionService = new DefaultFormattingConversionService();
		Foo foo = new Foo();
		foo.setBar("bar");
		try {
			conversionService.convert(new Foo(), String.class);
			fail("should throw exception here");
		}
		catch (Exception e) {

		}
		DefaultContentTypeAwareConverterRegistry converterRegistry = new DefaultContentTypeAwareConverterRegistry();
		Converter<?, ?> jsonConverter = converterRegistry.getConverters(
				MediaType.APPLICATION_JSON).get(Object.class);
		conversionService.addConverter(jsonConverter);
		assertEquals("{\"bar\":\"bar\"}", conversionService.convert(foo, String.class));

	}

	static class Foo {

		private String bar;


		public String getBar() {
			return bar;
		}


		public void setBar(String bar) {
			this.bar = bar;
		}

		@Override
		public String toString() {
			return bar + "!";
		}
	}
}
