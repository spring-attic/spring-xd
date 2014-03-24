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

import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.xd.analytics.ml.Analytic;
import org.springframework.xd.tuple.Tuple;

/**
 * @author Thomas Darimont
 */
public class NaiveBayesClassificationPmmlAnalyticTest extends AbstractPmmlAnalyticTest {

	/**
	 * @see XD-1420
	 */
	@Test
	public void testNaiveBayesClassificationIris1() throws Exception {

		Analytic<Tuple, Tuple> analytic = useAnalytic("iris-flower-classification-naive-bayes-1",null, Arrays.asList("Predicted_Species:predictedSpecies"));

		Tuple output = analytic.evaluate(objectToTuple(new Object() {
			@Value("Sepal.Length") double sepalLength = 6.4;
			@Value("Sepal.Width") double sepalWidth = 3.2;
			@Value("Petal.Length") double petalLength = 4.5;
			@Value("Petal.Width") double petalWidth = 1.5;
		}));

		assertThat(output.getString("predictedSpecies"), is("versicolor"));

		output = analytic.evaluate(objectToTuple(new Object() {
			@Value("Sepal.Length") double sepalLength = 6.9;
			@Value("Sepal.Width") double sepalWidth = 3.1;
			@Value("Petal.Length") double petalLength = 5.4;
			@Value("Petal.Width") double petalWidth = 2.1;
		}));

		assertThat(output.getString("predictedSpecies"), is("virginica"));
	}
}
