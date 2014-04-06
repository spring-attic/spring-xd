/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.xd.analytics.ml;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.util.Assert;

/**
 * @author Thomas Darimont
 */
public abstract class AbstractFieldMappingAwareDataMapper implements Mapper{

	public static final String DEFAULT_FIELD_NAME_MAPPING_SPLIT_TOKEN = ":";

	private final String fieldMappingEntrySplitToken;

	/**
	 * Creates a new {@link AbstractFieldMappingAwareDataMapper} with the {@link #DEFAULT_FIELD_NAME_MAPPING_SPLIT_TOKEN}.
	 */
	public AbstractFieldMappingAwareDataMapper(){
		this(DEFAULT_FIELD_NAME_MAPPING_SPLIT_TOKEN);
	}

	/**
	 * Creates a new {@link AbstractFieldMappingAwareDataMapper} with the {@link #DEFAULT_FIELD_NAME_MAPPING_SPLIT_TOKEN}.
	 *
	 * @param fieldMappingEntrySplitToken the fieldMappingEntry must not be {@literal null}.
	 */
	public AbstractFieldMappingAwareDataMapper(String fieldMappingEntrySplitToken){

		Assert.notNull(fieldMappingEntrySplitToken, "FieldMappingEntrySplitToken must not be null!");

		this.fieldMappingEntrySplitToken = fieldMappingEntrySplitToken;
	}

	/**
	 * Transforms the list of field name mapping pairs to a {@link java.util.Map} from a name to a new name.
	 *
	 * @param fieldNameMappingPairs
	 * @return
	 */
	protected Map<String,String> extractFieldNameMappingFrom(List<String> fieldNameMappingPairs){

		Assert.notNull(fieldNameMappingPairs, "fieldNameMappingPairs");

		Map<String,String> mapping = new LinkedHashMap<String,String>();

		for (String fieldNameMapping : fieldNameMappingPairs) {

			String fieldNameFrom = fieldNameMapping;
			String fieldNameTo = fieldNameMapping;

			if (fieldNameMapping.contains(fieldMappingEntrySplitToken)) {
				String[] fromTo = fieldNameMapping.split(fieldMappingEntrySplitToken);
				fieldNameFrom = fromTo[0];
				fieldNameTo = fromTo[1];
			}

			mapping.put(fieldNameFrom,fieldNameTo);
		}

		return mapping;
	}

	public String getFieldMappingEntrySplitToken() {
		return fieldMappingEntrySplitToken;
	}
}
