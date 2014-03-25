package org.springframework.xd.analytics.ml;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.springframework.xd.tuple.Tuple;
import org.springframework.xd.tuple.TupleBuilder;

/**
 * @author Thomas Darimont
 */
public class AnalyticProcessorTest {

    AnalyticProcessor analyticProcessor;

    @Before
    public void setup() {
        analyticProcessor = new AnalyticProcessor();
    }

    /**
     * @see XD-1418
     */
    @Test
    public void testProcessWithNoAnalyticConfiguredShouldReturnInput(){

        Tuple input = TupleBuilder.tuple().of("k1", "v1", "k2", "v2");

        Tuple output = analyticProcessor.process(input);

        assertSame(input,output);
    }

    /**
     * @see XD-1418
     */
    @Test
    public void testProcessWithAnalyticConfiguredShouldReturnNewTuple(){

        analyticProcessor.setAnalytic(new DummyAnalytic());

        Tuple input = TupleBuilder.tuple().of("k1", "v1", "k2", "v2");
        Tuple output = analyticProcessor.process(input);

        assertNotEquals(input, output);
        assertThat(output.getString("k3"),is("v3"));
    }
}
