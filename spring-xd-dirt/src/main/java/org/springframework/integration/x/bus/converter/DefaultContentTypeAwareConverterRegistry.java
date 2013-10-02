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

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.springframework.http.MediaType.valueOf;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.core.convert.converter.Converter;
import org.springframework.http.MediaType;
import org.springframework.util.ClassUtils;
import org.springframework.xd.tuple.DefaultTuple;
import org.springframework.xd.tuple.Tuple;


/**
 * 
 * @author David Turanski
 */
public class DefaultContentTypeAwareConverterRegistry implements ContentTypeAwareConverterRegistry {

	private final Map<MediaType, Map<Class<?>, Converter<?, ?>>> converters = new LinkedHashMap<MediaType, Map<Class<?>, Converter<?, ?>>>();

	public DefaultContentTypeAwareConverterRegistry() {
		addConverter(String.class, X_XD_TUPLE, new JsonToTupleConverter());
		MediaType nativeTupleType = valueOf("application/x-java-object;type=" + Tuple.class.getName());
		addConverter(String.class, nativeTupleType, new JsonToTupleConverter());
		addConverter(String.class, valueOf("application/x-java-object;type=" + Map.class.getName()),
				new JsonToMapConverter());
		addConverter(String.class, valueOf("application/x-java-object;type=" + HashMap.class.getName()),
				new JsonToMapConverter());
		addConverter(Tuple.class, APPLICATION_JSON, new TupleToJsonConverter());
		addConverter(Tuple.class, TEXT_PLAIN, new TupleToJsonConverter());
		addConverter(Object.class, TEXT_PLAIN, new DefaultObjectToStringConverter());
		addConverter(Object.class, APPLICATION_JSON, new MappingJackson2Converter());
		addConverter(Object.class, ContentTypeAwareConverterRegistry.X_JAVA_SERIALIZED_OBJECT,
				new JavaSerializingConverter());
		addConverter(byte[].class, TEXT_PLAIN, new ByteArrayToStringConverter());
	}

	@Override
	public final void addConverter(Class<?> sourceType, MediaType targetContentType,
			Converter<?, ?> converter) {
		if (!converters.containsKey(targetContentType)) {
			converters.put(targetContentType, new LinkedHashMap<Class<?>, Converter<?, ?>>());
		}
		converters.get(targetContentType).put(sourceType, converter);
	}

	@Override
	public final Converter<?, ?> getConverter(Class<?> sourceType, MediaType targetContentType) {
		if (!converters.containsKey(targetContentType)) {
			return null;
		}
		return converters.get(targetContentType).get(sourceType);
	}

	public Class<?> getJavaTypeForContentType(MediaType contentType, ClassLoader classLoader) {
		if (X_JAVA_OBJECT.includes(contentType)) {
			if (contentType.getParameter("type") != null) {
				try {
					return ClassUtils.forName(contentType.getParameter("type"), classLoader);
				}
				catch (Throwable t) {
					throw new ConversionException(t.getMessage(), t);
				}
			}
			else {
				return Object.class;
			}
		}
		else if (APPLICATION_JSON.equals(contentType)) {
			return String.class;
		}
		else if (valueOf("text/*").includes(contentType)) {
			return String.class;
		}
		else if (X_XD_TUPLE.includes(contentType)) {
			return DefaultTuple.class;
		}
		else if (APPLICATION_OCTET_STREAM.includes(contentType)) {
			return byte[].class;
		}
		else if (X_JAVA_SERIALIZED_OBJECT.includes(contentType)) {
			return byte[].class;
		}
		return null;
	}

	@Override
	public Map<Class<?>, Converter<?, ?>> getConverters(MediaType targetContentType) {
		MediaType lookupContentType = targetContentType;

		// Handle input like "text/plain;charset=UTF-8"
		if (MediaType.TEXT_PLAIN.includes(targetContentType)) {
			lookupContentType = MediaType.TEXT_PLAIN;
		}
		return converters.get(lookupContentType);
	}
}
