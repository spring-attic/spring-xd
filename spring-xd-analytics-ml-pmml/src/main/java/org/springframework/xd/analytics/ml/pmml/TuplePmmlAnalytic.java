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

package org.springframework.xd.analytics.ml.pmml;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.base.Splitter;
import org.dmg.pmml.FieldName;
import org.springframework.xd.analytics.ml.InputMapper;
import org.springframework.xd.analytics.ml.OutputMapper;
import org.springframework.xd.tuple.Tuple;

/**
 * A {@link org.springframework.xd.analytics.ml.pmml.PmmlAnalytic} that works with
 * {@link org.springframework.xd.tuple.Tuple}s.
 * 
 * @author Thomas Darimont
 */
public class TuplePmmlAnalytic extends PmmlAnalytic<Tuple, Tuple> {

	private static final Splitter FIELD_MAPPING_SPLITTER = Splitter.on(',').omitEmptyStrings().trimResults();

	/**
	 * Creates a new {@link org.springframework.xd.analytics.ml.pmml.TuplePmmlAnalytic}.
	 * 
	 * @param name must not be {@literal null}.
	 * @param pmmlResolver must not be {@literal null}.
	 * @param inputMapper must not be {@literal null}.
	 * @param outputMapper must not be {@literal null}.
	 */
	public TuplePmmlAnalytic(String name, PmmlResolver pmmlResolver,
			InputMapper<Tuple, PmmlAnalytic<Tuple, Tuple>, Map<FieldName, Object>> inputMapper,
			OutputMapper<Tuple, Tuple, PmmlAnalytic<Tuple, Tuple>, Map<FieldName, Object>> outputMapper) {
		super(name, pmmlResolver, inputMapper, outputMapper);
	}

	/**
	 * Creates a new {@link org.springframework.xd.analytics.ml.pmml.TuplePmmlAnalytic}. Convenience constructor that
	 * takes comma-separated {@link String}s as fieldname mappings.
	 * 
	 * The 3 support variants for defining fieldname mappings have the form:
	 * 
	 * <pre>
	 * 
	 *  Variant 1: Source field to target field mapping
	 * 	sourceFieldName1:targetFieldName1, sourceFieldName2:targetFieldName2,...
	 * 
	 * 	Variant 2: Listing of field names
	 * 	sourceFieldName1, sourceFieldName2, sourceFieldName3, ...
	 * 
	 * 	Variant 3: Defining no field names
	 * -> means all fields present will be present in the output
	 * 
	 * <pre>
	 * 
	 * @see {@link #TuplePmmlAnalytic(String, PmmlResolver, String, String)}
	 * 
	 * @param name must not be {@literal null}.
	 * @param pmmlResolver must not be {@literal null}.
	 * @param inputFieldMappings
	 * @param outputFieldMappings
	 */
	public TuplePmmlAnalytic(String name, PmmlResolver pmmlResolver, String inputFieldMappings,
			String outputFieldMappings) {
		this(name, pmmlResolver, new TuplePmmlAnalyticInputMapper(splitFieldMappings(inputFieldMappings)),
				new TuplePmmlAnalyticOutputMapper(splitFieldMappings(outputFieldMappings)));
	}

	/**
	 * Splits the field mappings of the form {@literal inputField1:outputField1,inputField1:outputField1} into a
	 * {@link java.util.List}.
	 * 
	 * @param fieldMappings
	 * @return a {@link java.util.List} containing the field mappings as elements or an empty {@code List} if
	 *         {@code fieldMappings} were {@literal null}.
	 */
	private static List<String> splitFieldMappings(String fieldMappings) {

		if (fieldMappings == null) {
			return Collections.<String> emptyList();
		}

		return FIELD_MAPPING_SPLITTER.splitToList(fieldMappings);
	}
}
