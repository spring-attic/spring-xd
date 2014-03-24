package org.springframework.xd.analytics.ml.pmml;

import java.util.List;

import org.junit.Before;
import org.springframework.xd.analytics.ml.Analytic;
import org.springframework.xd.tuple.Tuple;

/**
 * @author Thomas Darimont
 */
public class AbstractPmmlAnalyticTest {

	protected PmmlResolver pmmlResolver;

	@Before
	public void setUp() throws Exception {
		pmmlResolver = new ClasspathPmmlResolver("analytics/pmml");
	}

	protected Analytic<Tuple, Tuple> useAnalytic(String analyticName, List<String> inputFieldNames, List<String> outputFieldNames) {
		return this.useAnalytic(analyticName, pmmlResolver, new PmmlAnalyticTupleInputMapper(inputFieldNames), new PmmlAnalyticTupleOutputMapper(outputFieldNames));
	}

	protected Analytic<Tuple, Tuple> useAnalytic(String analyticName, PmmlResolver pmmlResolver, PmmlAnalyticTupleInputMapper inputMapper, PmmlAnalyticTupleOutputMapper outputMapper) {
		return new PmmlAnalytic<Tuple, Tuple>(analyticName, pmmlResolver, inputMapper, outputMapper);
	}

	protected PmmlResolver getPmmlResolver() {
		return pmmlResolver;
	}
}
