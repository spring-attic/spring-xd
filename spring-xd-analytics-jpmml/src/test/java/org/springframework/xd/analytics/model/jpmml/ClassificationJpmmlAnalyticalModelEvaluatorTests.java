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
public class ClassificationJpmmlAnalyticalModelEvaluatorTests extends AbstractJpmmlAnalyticalModelEvaluatorTests {

	@Before
	public void setup() throws Exception {

		analyticalModelEvaluator = new JpmmlAnalyticalModelEvaluator();
	}

	@Test
	public void testEvaluateNaiveBayesClassification1Iris() throws Exception {

		useModel("naive-bayes-classification-1-iris.pmml.xml", null, Arrays.asList("Predicted_Species"));

		Tuple output = analyticalModelEvaluator.evaluate(objectToTuple(new Object() {
			@Value("Sepal.Length") double sepalLength = 6.4;
			@Value("Sepal.Width") double sepalWidth = 3.2;
			@Value("Petal.Length") double petalLength = 4.5;
			@Value("Petal.Width") double petalWidth = 1.5;
		}));

		assertThat(output.getString("Predicted_Species"), is("versicolor"));

		output = analyticalModelEvaluator.evaluate(objectToTuple(new Object() {
			@Value("Sepal.Length") double sepalLength = 6.9;
			@Value("Sepal.Width") double sepalWidth = 3.1;
			@Value("Petal.Length") double petalLength = 5.4;
			@Value("Petal.Width") double petalWidth = 2.1;
		}));

		assertThat(output.getString("Predicted_Species"), is("virginica"));
	}
}
