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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.springframework.xd.analytics.ml.pmml.TupleTestUtils.*;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.springframework.xd.analytics.ml.Analytic;
import org.springframework.xd.tuple.Tuple;

/**
 * @author Thomas Darimont
 */
public class AssociationAnalysisPmmlAnalyticTest extends AbstractPmmlAnalyticTest {

	/**
	 * @see XD-1420
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void testEvaluateAssociationRules1shopping() {

		Analytic<Tuple, Tuple> analytic = useAnalytic("shopping-association-rules-1", getPmmlResolver(), new PmmlAnalyticTupleInputMapper(null), new PmmlAnalyticAssociationTupleOutputMapper(Arrays.asList("Predicted_item")));

		Tuple output = analytic.evaluate(objectToTuple(new Object() {
			Collection<String> item = Arrays.asList("Choclates");
		}));

		Collection<String> predicted = (Collection<String>) output.getValue("Predicted_item");
		assertThat(predicted, hasItems("Pencil"));
	}
}
