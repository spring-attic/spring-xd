
package org.springframework.xd.analytics.ml;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.xd.tuple.Tuple;
import org.springframework.xd.tuple.TupleBuilder;

/**
 * @author Thomas Darimont
 */
public class MappedAnalyticTest {

	Analytic<Tuple, Tuple> analytic;

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Before
	public void setup() {
		analytic = new DummyMappedAnalytic(new DummyInputMapper(), new DummyOutputMapper());
	}

	/**
	 * @see XD-1418
	 */
	@Test
	public void testShouldNotAllowNullInputMapper() {

		expectedException.expect(IllegalArgumentException.class);
		expectedException.expectMessage("inputMapper");

		new DummyMappedAnalytic(null, new DummyOutputMapper());
	}

	/**
	 * @see XD-1418
	 */
	@Test
	public void testShouldNotAllowNullOutputMapper() {

		expectedException.expect(IllegalArgumentException.class);
		expectedException.expectMessage("outputMapper");

		new DummyMappedAnalytic(new DummyInputMapper(), null);
	}

	/**
	 * @see XD-1418
	 */
	@Test
	public void testEvaluateDummyMappedAnalytic() {

		Tuple input = TupleBuilder.tuple().of("k1", "v1", "k2", "v2");

		Tuple output = analytic.evaluate(input);

		assertNotSame(input, output);
		assertThat(output.getString("k1"), is("V1"));
		assertThat(output.getString("k2"), is("V2"));
	}
}
