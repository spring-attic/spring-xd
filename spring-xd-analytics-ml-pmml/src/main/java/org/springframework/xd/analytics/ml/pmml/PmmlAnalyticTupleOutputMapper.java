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

import java.util.*;

import org.dmg.pmml.FieldName;
import org.springframework.util.Assert;
import org.springframework.xd.analytics.ml.OutputMapper;
import org.springframework.xd.tuple.Tuple;
import org.springframework.xd.tuple.TupleBuilder;

/**
 * @author Thomas Darimont
 */
public class PmmlAnalyticTupleOutputMapper implements OutputMapper<Tuple, Tuple, PmmlAnalytic<Tuple, Tuple>, Map<FieldName, Object>> {

	private final Map<String, String> resultFieldToOutputFieldNameMapping;

	private final List<FieldName> resultFields;

	/**
	 * Creates a new {@link PmmlAnalyticTupleOutputMapper}.
	 *
	 * @param resultFieldToOutputFieldNameMapping
	 */
	public PmmlAnalyticTupleOutputMapper(List<String> resultFieldToOutputFieldNameMapping) {

		if (resultFieldToOutputFieldNameMapping == null) {
			this.resultFieldToOutputFieldNameMapping = null;
			this.resultFields = null;
			return;
		}

		this.resultFieldToOutputFieldNameMapping = new HashMap<String, String>(resultFieldToOutputFieldNameMapping.size());
		this.resultFields = new ArrayList<FieldName>(resultFieldToOutputFieldNameMapping.size());

		registerOutputFieldMapping(resultFieldToOutputFieldNameMapping);
	}

	/**
	 * @param resultFieldToOutputFieldNameMapping must not be {@literal null}.
	 */
	private void registerOutputFieldMapping(List<String> resultFieldToOutputFieldNameMapping) {

		Assert.notNull(resultFieldToOutputFieldNameMapping, "resultFieldToOutputFieldNameMapping");

		for (String fieldNameMapping : resultFieldToOutputFieldNameMapping) {

			String fieldNameFrom = fieldNameMapping;
			String fieldNameTo = fieldNameMapping;

			if (fieldNameMapping.contains(":")) {
				String[] fromTo = fieldNameMapping.split(":");
				fieldNameFrom = fromTo[0];
				fieldNameTo = fromTo[1];
			}

			this.resultFieldToOutputFieldNameMapping.put(fieldNameFrom, fieldNameTo);
			this.resultFields.add(new FieldName(fieldNameFrom));
		}
	}

	/**
	 * @param analytic the {@link PmmlAnalytic} that can be used to retrieve mapping information.
	 * @param modelOutput
	 * @param input the input for this {@link PmmlAnalytic} that could be used to compute the new output {@code O}.
	 * @return
	 */
	@Override
	public Tuple mapOutput(PmmlAnalytic<Tuple, Tuple> analytic, Tuple input, Map<FieldName, Object> modelOutput) {

		List<String> outputNames = new ArrayList<String>(input.getFieldNames());
		List<Object> outputValues = new ArrayList<Object>(input.getValues());

		enhanceResultIfNecessary(analytic, resultFields, modelOutput);

		addEntriesFromResult(modelOutput, outputNames, outputValues);

		return TupleBuilder.tuple().ofNamesAndValues(outputNames, outputValues);
	}

	/**
	 * Sub-classes can customize the model-output before it is mapped to a {@link org.springframework.xd.tuple.Tuple} if necessary.
	 *
	 * @param analytic
	 * @param outputFields
	 * @param modelOutput
	 */
	protected void enhanceResultIfNecessary(PmmlAnalytic<Tuple, Tuple> analytic, List<FieldName> outputFields, Map<FieldName, Object> modelOutput) {
		//NOOP
	}

	/**
	 * @param modelOutput
	 * @param outputNames
	 * @param outputValues
	 */
	protected void addEntriesFromResult(Map<FieldName, ? super Object> modelOutput, List<String> outputNames, List<Object> outputValues) {

		Collection<FieldName> resultFieldNames = resultFields == null ? modelOutput.keySet() : resultFields;

		for (FieldName resultField : resultFieldNames) {

			Object outputValue = modelOutput.get(resultField);
			String outputFieldName = resultFieldToOutputFieldNameMapping == null ? resultField.getValue() : resultFieldToOutputFieldNameMapping.get(resultField.getValue());

			int fieldIndex = outputNames.indexOf(outputFieldName);
			if (fieldIndex != -1) {
				outputValues.set(fieldIndex, outputValue);
			} else {
				outputNames.add(outputFieldName);
				outputValues.add(outputValue);
			}
		}
	}
}
