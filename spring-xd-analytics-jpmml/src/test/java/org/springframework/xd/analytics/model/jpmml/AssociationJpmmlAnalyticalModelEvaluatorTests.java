package org.springframework.xd.analytics.model.jpmml;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.springframework.xd.tuple.Tuple;

/**
 * Author: Thomas Darimont
 */
public class AssociationJpmmlAnalyticalModelEvaluatorTests extends AbstractJpmmlAnalyticalModelEvaluatorTests {

	@Before
	public void setup() throws Exception {

		analyticalModelEvaluator = new JpmmlAnalyticalModelEvaluator();
	}

	@Test
	public void testEvaluateAssociationRules1shopping() throws Exception {

		useModel("association-rules-1-shopping.pmml.xml", null, Arrays.asList("Predicted_item"));

		Tuple output = analyticalModelEvaluator.evaluate(objectToTuple(new Object() {
			Collection<String> item= Arrays.asList("Choclates");
		}));

		Collection<String> predicted = (Collection<String>)output.getValue("Predicted_item");
		assertThat(predicted, hasItems("Pencil"));
	}
}
