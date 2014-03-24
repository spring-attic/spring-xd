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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.dmg.pmml.*;
import org.jpmml.evaluator.Association;
import org.springframework.xd.tuple.Tuple;

/**
 * @author Thomas Darimont
 */
public class PmmlAnalyticAssociationTupleOutputMapper extends PmmlAnalyticTupleOutputMapper {

	/**
	 * Creates a new {@link PmmlAnalyticAssociationTupleOutputMapper}.
	 *
	 * @param outputFieldsNames
	 */
	public PmmlAnalyticAssociationTupleOutputMapper(List<String> outputFieldsNames) {
		super(outputFieldsNames);
	}

	/**
	 * @param analytic
	 * @param outputFields
	 * @param modelOutput
	 */
	@Override
	protected void enhanceResultIfNecessary(PmmlAnalytic<Tuple, Tuple> analytic, List<FieldName> outputFields, Map<FieldName, Object> modelOutput) {

		Model pmmlModel = analytic.getDefaultModel();

		if (pmmlModel instanceof AssociationModel) {
			AssociationModel ass = (AssociationModel) pmmlModel;

			Association assoc = (Association) modelOutput.get(null);

			List<String> itemIds = new ArrayList<String>();
			String itemSetId = assoc.getAssociationRules().get(0).getConsequent();
			for (ItemRef itemRef : assoc.getItemsetRegistry().get(itemSetId).getItemRefs()) {

				Item item = assoc.getItemRegistry().get(itemRef.getItemRef());
				itemIds.add(item.getValue());
			}

			modelOutput.put(outputFields.iterator().next(), itemIds);
		}
	}
}
