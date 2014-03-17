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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.xd.tuple.Tuple;

/**
 * Author: Thomas Darimont
 */
public class ClusteringJpmmlAnalyticalModelEvaluatorTests extends AbstractJpmmlAnalyticalModelEvaluatorTests {

	@Before
	public void setup() throws Exception {

		analyticalModelEvaluator = new JpmmlAnalyticalModelEvaluator();
	}

	@Test
	public void testEvaluateNaiveBayesClassification1Iris() throws Exception {

		useModel("kmeans-clustering-1-iris.pmml.xml", null, Arrays.asList("predictedValue"));

		Tuple output = analyticalModelEvaluator.evaluate(objectToTuple(new Object() {
			@Value("Sepal.Length") double sepalLength = 6.4;
			@Value("Sepal.Width") double sepalWidth = 3.2;
			@Value("Petal.Length") double petalLength = 4.5;
			@Value("Petal.Width") double petalWidth = 1.5;
		}));

		assertThat(output.getString("predictedValue"), is("1")); //versicolor

		output = analyticalModelEvaluator.evaluate(objectToTuple(new Object() {
			@Value("Sepal.Length") double sepalLength = 6.9;
			@Value("Sepal.Width") double sepalWidth = 3.1;
			@Value("Petal.Length") double petalLength = 5.4;
			@Value("Petal.Width") double petalWidth = 2.1;
		}));

		assertThat(output.getString("predictedValue"), is("3")); //virginica
	}
}
