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
 * Strategy interface for a implementing a registry of {@link Converter}s mapped to {@link MediaType} Used by XD to
 * resolve a Converter corresponding to a requested MediaType.
 * 
 * The registry supports multiple implementations of converters from a given source type to a target type for different
 * MediaTypes. For example, there may be JSON and XML implementations of Converter<Object,String>, corresponding to
 * application/json and application/xml, respectively.
 * 
 * 
 * @author David Turanski
 * @since 1.0
 */
public interface ContentTypeAwareConverterRegistry {

	/**
	 * An XD MediaType specifying a {@link Tuple}
	 */
	public static final MediaType X_XD_TUPLE = MediaType.valueOf("application/x-xd-tuple");

	/**
	 * A general MediaType for Java Types
	 */
	public static final MediaType X_JAVA_OBJECT = MediaType.valueOf("application/x-java-object");

	/**
	 * A general MediaType for a Java serialized byte array
	 */
	public static final MediaType X_JAVA_SERIALIZED_OBJECT = MediaType.valueOf("application/x-java-serialized-object");

	/**
	 * Register a converter
	 * 
	 * @param sourceType the class of the source type
	 * @param targetContentType the MediaType associated with the converter's target type
	 * @param converter a Converter<S,T> that can convert from source type S to target type T
	 */
	public void addConverter(Class<?> sourceType, MediaType targetContentType, Converter<?, ?> converter);

	/**
	 * return all registered converters for a given content type. Implementations may include converters for types which
	 * include the targetContentType
	 * 
	 * For example if "text/plain;charset=UTF-8" is passed as an argument, the output may also include converters
	 * registered under "text/plain"
	 * 
	 * @param targetContentType
	 * @return the converters if any or null
	 */
	public Map<Class<?>, Converter<?, ?>> getConverters(MediaType targetContentType);
}
