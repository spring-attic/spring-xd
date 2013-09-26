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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.apache.commons.lang.ClassUtils;

import org.springframework.core.convert.converter.Converter;
import org.springframework.http.MediaType;


/**
 * 
 * @author David Turanski
 */
// TODO: Revisit MessageConverters
public abstract class ContentTypeAwareConverter<S, T> implements Converter<S, T> {

	private final Class<?> sourceClass;

	private final Class<?> targetClass;

	public ContentTypeAwareConverter() {
		Type type = this.getClass().getGenericSuperclass();
		ParameterizedType parameterizedType = (ParameterizedType) type;
		sourceClass = (Class) parameterizedType.getActualTypeArguments()[0];
		targetClass = (Class) parameterizedType.getActualTypeArguments()[1];
	}

	public boolean canConvert(Class<?> source, MediaType sourceContentType, Class<?> target,
			MediaType targetContentType) {

		if (ClassUtils.isAssignable(sourceClass, source) && ClassUtils.isAssignable(targetClass, target)) {
			return doCanConvert(source, sourceContentType, target, targetContentType);
		}
		return false;
	}

	protected abstract boolean doCanConvert(Class<?> source, MediaType sourceContentType, Class<?> target,
			MediaType targetContentType);
}
