/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.xd.analytics.model.jpmml;

import java.util.*;

import org.dmg.pmml.*;
import org.jpmml.evaluator.Association;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.ModelEvaluatorFactory;
import org.jpmml.manager.PMMLManager;
import org.springframework.xd.analytics.model.AbstractAnalyticalModel;
import org.springframework.xd.tuple.Tuple;
import org.springframework.xd.tuple.TupleBuilder;

/**
 * Author: Thomas Darimont
 */
public class JpmmlAnalyticalModel extends AbstractAnalyticalModel{

	private final PMML pmml;

	private Set<String> inputFields;

	private List<String> outputFieldsNames = new ArrayList<String>();

	private List<FieldName> outputFields;

	private Evaluator evaluator;

	public JpmmlAnalyticalModel(PMML pmml){
		this.pmml = pmml;
	}

	public Set<String> getInputFields() {
		return inputFields;
	}

	public void setInputFields(Set<String> inputFields) {
		this.inputFields = inputFields;
	}

	public List<String> getOutputFieldsNames() {
		return Collections.unmodifiableList(outputFieldsNames);
	}

	public void setOutputFieldsNames(List<String> outputFieldsNames) {
		this.outputFieldsNames = outputFieldsNames;

		this.outputFields = new ArrayList<FieldName>(outputFieldsNames.size());
		for(String fieldName : outputFieldsNames){
			this.outputFields.add(new FieldName(fieldName));
		}
	}

	private Map<FieldName, Object> extractInputDataFrom(Tuple input) {

		Map<FieldName, Object> inputData = new HashMap<FieldName, Object>();
		for (String fieldName : input.getFieldNames()) {

			if (inputFields == null || inputFields.contains(fieldName)) {
				inputData.put(new FieldName(fieldName), input.getValue(fieldName));
			}
		}

		return inputData;
	}

	private Tuple buildNewOutputTupleFrom(Map<FieldName, ? super Object> result, Tuple input) {

		List<String> outputNames = new ArrayList(input.getFieldNames());
		List<Object> outputValues = new ArrayList<Object>(input.getValues());

		enhanceResultIfNecessary(result);

		addEntriesFromResult(result, outputNames, outputValues);

		return TupleBuilder.tuple().ofNamesAndValues(outputNames, outputValues);
	}

	private void enhanceResultIfNecessary(Map<FieldName, ? super Object> result) {

		Model model = getDefaultModel();
		if(model instanceof AssociationModel){
			AssociationModel ass = (AssociationModel)model;

			Association assoc = (Association)result.get(null);

			List<String> itemIds = new ArrayList<String>();
			String itemSetId = assoc.getAssociationRules().get(0).getConsequent();
			for(ItemRef itemRef : assoc.getItemsetRegistry().get(itemSetId).getItemRefs()){

				Item item = assoc.getItemRegistry().get(itemRef.getItemRef());
				itemIds.add(item.getValue());
			}

			result.put(outputFields.iterator().next(), itemIds);
		}
	}

	private void addEntriesFromResult(Map<FieldName, ? super Object> result, List<String> outputNames, List<Object> outputValues) {

		for (FieldName field : outputFields) {

			Object outputValue = result.get(field);

			int fieldIndex = outputNames.indexOf(field.getValue());
			if (fieldIndex != -1) {
				outputValues.set(fieldIndex, outputValue);
			} else {
				outputNames.add(field.getValue());
				outputValues.add(outputValue);
			}
		}
	}

	private Model getDefaultModel() {
		return this.pmml.getModels().get(0);
	}

	public void init(){
		this.evaluator = this.initEvaluator();
	}

	private Evaluator initEvaluator() {
		return (Evaluator) new PMMLManager(this.pmml).getModelManager(null, ModelEvaluatorFactory.getInstance());
	}

	public Evaluator getEvaluator() {
		return evaluator;
	}

	public Tuple evaluate(Tuple input) {

		Map<FieldName, Object> data = extractInputDataFrom(input);

		Map<FieldName, Object> result = (Map<FieldName, Object>) getEvaluator().evaluate(data);

		Tuple output = buildNewOutputTupleFrom(result, input);

		return output;
	}
}
