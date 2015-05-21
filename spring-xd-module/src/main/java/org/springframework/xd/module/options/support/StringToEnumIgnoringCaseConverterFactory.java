/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.xd.module.options.support;

import java.util.EnumSet;
import java.util.Set;

import org.springframework.boot.bind.RelaxedNames;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Copied from {@code org.springframework.boot.bind.RelaxedConversionService}.
 *
 * @author Gunnar Hillert
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class StringToEnumIgnoringCaseConverterFactory implements
		ConverterFactory<String, Enum> {

	@Override
	public <T extends Enum> Converter<String, T> getConverter(Class<T> targetType) {
		Assert.notNull(targetType, "The target type must not be null.");

		Class<?> enumType = targetType;
		while (enumType != null && !enumType.isEnum()) {
			enumType = enumType.getSuperclass();
		}
		Assert.notNull(enumType, "The target type " + targetType.getName()
				+ " does not refer to an enum");
		return new StringToEnum(enumType);
	}

	private static class StringToEnum<T extends Enum> implements Converter<String, T> {

		private final Class<T> enumType;

		public StringToEnum(Class<T> enumType) {
			this.enumType = enumType;
		}

		@Override
		public T convert(String source) {
			if (StringUtils.isEmpty(source)) {
				return null;
			}
			source = source.trim();
			for (T candidate : (Set<T>) EnumSet.allOf(this.enumType)) {
				RelaxedNames names = new RelaxedNames(candidate.name()
						.replace("_", "-").toLowerCase());
				for (String name : names) {
					if (name.equals(source)) {
						return candidate;
					}
				}
				if (candidate.name().equalsIgnoreCase(source)) {
					return candidate;
				}
			}
			throw new ConversionFailedException(TypeDescriptor.valueOf(String.class),
					TypeDescriptor.valueOf(enumType), source, null);
		}
	}

}
