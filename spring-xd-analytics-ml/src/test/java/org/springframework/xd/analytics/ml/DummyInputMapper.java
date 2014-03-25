package org.springframework.xd.analytics.ml;

import java.util.HashMap;
import java.util.Map;

import org.springframework.xd.tuple.Tuple;

/**
 * @author Thomas Darimont
 */
public class DummyInputMapper implements InputMapper<Tuple, DummyMappedAnalytic,Map<String,String>> {

    @Override
    public Map<String, String> mapInput(DummyMappedAnalytic analytic, Tuple input) {

        Map<String,String> newInput = new HashMap<String,String>();

        for(String fieldName : input.getFieldNames()){
            newInput.put(fieldName, input.getString(fieldName));
        }

        return newInput;
    }
}
