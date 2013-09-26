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

import java.util.Map;

import org.springframework.core.convert.converter.Converter;
import org.springframework.http.MediaType;


/**
 * 
 * @author David Turanski
 */
public interface ContentTypeAwareConverterRegistry {

	public static final MediaType X_XD_TUPLE = MediaType.valueOf("application/x-xd-tuple");

	public static final MediaType X_JAVA_OBJECT = MediaType.valueOf("application/x-java-object");

	public static final MediaType X_JAVA_SERIALIZED_OBJECT = MediaType.valueOf("application/x-java-serialized-object");

	public void addConverter(Class<?> sourceType, MediaType targetContentType, Converter<?, ?> converter);

	public Converter<?, ?> getConverter(Class<?> sourceType, MediaType targetContentType);

	public Map<Class<?>, Converter<?, ?>> getConverters(MediaType targetContentType);
}
