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
import org.springframework.util.Assert;
import org.springframework.xd.tuple.Tuple;

/**
 * An {@link TuplePmmlAnalyticOutputMapper} that can map the output of a {@link org.dmg.pmml.PMML} model evaluation
 * of an Association model to a {@link org.springframework.xd.tuple.Tuple}.
 * Since it requires special output mapping as no output fields are created in the model.
 *
 * @author Thomas Darimont
 */
public class AssociationTuplePmmlAnalyticOutputMapper extends TuplePmmlAnalyticOutputMapper {

	/**
	 * Creates a new {@link AssociationTuplePmmlAnalyticOutputMapper}.
	 *
	 * @param outputFieldsNames
	 */
	public AssociationTuplePmmlAnalyticOutputMapper(List<String> outputFieldsNames) {
		super(outputFieldsNames);
	}

	/**
	 * Extracts the item recommendations from the result of an association model evaluation.
	 * The outputFields must contain exactly one field name. This field name will be used to store
	 * the list of the associated items.
	 *
	 * @param analytic must not be {@literal null}
	 * @param outputFields must not be {@literal null}
	 * @param modelOutput must not be {@literal null}
	 */
	@Override
	protected void enhanceResultIfNecessary(PmmlAnalytic<Tuple, Tuple> analytic, List<FieldName> outputFields, Map<FieldName, Object> modelOutput) {

		Assert.notNull(analytic, "analytic");
		Assert.notNull(modelOutput, "modelOutput");
		Assert.notNull(outputFields, "outputFields");
		Assert.state(outputFields.size() == 1, "output fields must contain 1 fieldName mapping only");

		Model pmmlModel = analytic.getDefaultModel();
		if (!(pmmlModel instanceof AssociationModel)) {
			super.enhanceResultIfNecessary(analytic, outputFields, modelOutput);
		}

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
