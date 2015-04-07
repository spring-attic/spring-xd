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
package org.springframework.core.convert.support;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.util.ObjectUtils;

/**
 * A copy of the {@link ArrayToArrayConverter}.  That class is a final class that uses the
 * {@link GenericConversionService#canBypassConvert(TypeDescriptor, TypeDescriptor)}
 * method explicitly to optimize conversions.  This version uses the equivalent method on
 * the {@link DefaultTupleConversionService}.
 *
 * @author Michael Minella
 */
public class XdArrayToArrayConverter implements ConditionalGenericConverter {

	private final CollectionToArrayConverter helperConverter;

	private final ConversionService conversionService;

	public XdArrayToArrayConverter(ConversionService conversionService) {
		this.helperConverter = new CollectionToArrayConverter(conversionService);
		this.conversionService = conversionService;
	}

	@Override
	public Set<GenericConverter.ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new GenericConverter.ConvertiblePair(Object[].class, Object[].class));
	}

	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return this.helperConverter.matches(sourceType, targetType);
	}

	@Override
	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (this.conversionService instanceof DefaultTupleConversionService &&
				((DefaultTupleConversionService) this.conversionService).canBypassConvert(
						sourceType.getElementTypeDescriptor(), targetType.getElementTypeDescriptor())) {
			return source;
		}
		List<Object> sourceList = Arrays.asList(ObjectUtils.toObjectArray(source));
		return this.helperConverter.convert(sourceList, sourceType, targetType);
	}

}
