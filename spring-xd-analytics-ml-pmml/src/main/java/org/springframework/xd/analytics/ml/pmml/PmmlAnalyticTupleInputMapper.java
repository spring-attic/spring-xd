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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dmg.pmml.FieldName;
import org.springframework.util.Assert;
import org.springframework.xd.analytics.ml.InputMapper;
import org.springframework.xd.tuple.Tuple;

/**
 * @author Thomas Darimont
 */
public class PmmlAnalyticTupleInputMapper implements InputMapper<Tuple, PmmlAnalytic<Tuple, Tuple>, Map<FieldName, Object>> {

	private final Map<String, String> inputFieldToModelInputNameMapping;

	/**
	 * Creates a new {@link PmmlAnalyticTupleInputMapper}.
	 *
	 * @param inputFieldNameMapping
	 */
	public PmmlAnalyticTupleInputMapper(List<String> inputFieldNameMapping) {

		if (inputFieldNameMapping == null) {
			this.inputFieldToModelInputNameMapping = null;
			return;
		}

		this.inputFieldToModelInputNameMapping = new HashMap<String, String>(inputFieldNameMapping.size());

		registerInputFieldMapping(inputFieldNameMapping);
	}

	/**
	 * @param inputFieldNameMapping must not be {@literal null}.
	 */
	private void registerInputFieldMapping(List<String> inputFieldNameMapping) {

		Assert.notNull(inputFieldNameMapping, "inputFieldNameMapping");

		for (String fieldNameMapping : inputFieldNameMapping) {

			String fieldNameFrom = fieldNameMapping;
			String fieldNameTo = fieldNameMapping;

			if (fieldNameMapping.contains(":")) {
				String[] fromTo = fieldNameMapping.split(":");
				fieldNameFrom = fromTo[0];
				fieldNameTo = fromTo[1];
			}

			this.inputFieldToModelInputNameMapping.put(fieldNameFrom, fieldNameTo);
		}
	}

	/**
	 * @param analytic must not be {@literal null}.
	 * @param input must not be {@literal null}.
	 * @return
	 */
	@Override
	public Map<FieldName, Object> mapInput(PmmlAnalytic<Tuple, Tuple> analytic, Tuple input) {

		Assert.notNull(analytic, "analytic");
		Assert.notNull(input, "input");

		Map<FieldName, Object> inputData = new HashMap<FieldName, Object>();
		for (String fieldName : input.getFieldNames()) {

			if (inputFieldToModelInputNameMapping == null || inputFieldToModelInputNameMapping.containsKey(fieldName)) {

				String modelInputFieldName = inputFieldToModelInputNameMapping == null ? fieldName : inputFieldToModelInputNameMapping.get(fieldName);
				inputData.put(new FieldName(modelInputFieldName), input.getValue(fieldName));
			}
		}

		return inputData;
	}
}
