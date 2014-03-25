package org.springframework.xd.analytics.ml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.xd.tuple.Tuple;
import org.springframework.xd.tuple.TupleBuilder;

/**
 * @author Thomas Darimont
 */
public class DummyOutputMapper implements OutputMapper<Tuple, Tuple, DummyMappedAnalytic,Map<String,String>> {


    /**
     * Ordering of fields in the resulting {@link org.springframework.xd.tuple.Tuple} can be different than the ordering of the input {@code Tuple}.
     *
     * @param analytic the {@link org.springframework.xd.analytics.ml.Analytic} that can be used to retrieve mapping information.
     * @param input the input for this {@link org.springframework.xd.analytics.ml.Analytic} that could be used to build the model {@code O}.
     * @param modelOutput the raw unmapped model output {@code MO}.
     * @return
     */
    @Override
    public Tuple mapOutput(DummyMappedAnalytic analytic, Tuple input, Map<String, String> modelOutput) {

        List<String> fieldNames = new ArrayList<String>();
        List<Object> values = new ArrayList<Object>();

        for(Map.Entry<String,String> entry : modelOutput.entrySet()){
            fieldNames.add(entry.getKey());
            values.add(entry.getValue());
        }

        return TupleBuilder.tuple().ofNamesAndValues(fieldNames, values);
    }

}
