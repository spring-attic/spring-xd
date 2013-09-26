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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import org.junit.Test;

import org.springframework.xd.tuple.Tuple;

/**
 * 
 * @author David Turanski
 */
public class ContentAwareConverterRegistryTests {

	@Test
	public void test() {
		DefaultContentTypeAwareConverterRegistry converterRegistry = new DefaultContentTypeAwareConverterRegistry();
		assertNotNull(converterRegistry.getConverter(Object.class, APPLICATION_JSON));
		assertTrue(converterRegistry.getConverter(Object.class, APPLICATION_JSON) instanceof MappingJackson2Converter);

		assertNotNull(converterRegistry.getConverter(Tuple.class, APPLICATION_JSON));
		assertTrue(converterRegistry.getConverter(Tuple.class, APPLICATION_JSON) instanceof TupleToJsonConverter);

		Class<?> javaType = converterRegistry.getJavaTypeForContentType(ContentTypeAwareConverterRegistry.X_XD_TUPLE,
				this.getClass().getClassLoader());
		assertTrue(Tuple.class.isAssignableFrom(javaType));

		javaType = converterRegistry.getJavaTypeForContentType(
				ContentTypeAwareConverterRegistry.X_JAVA_SERIALIZED_OBJECT, this.getClass().getClassLoader());
		assertTrue(byte[].class.equals(javaType));
	}
}
