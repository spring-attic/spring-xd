
package org.springframework.xd.analytics.ml;

import java.util.ArrayList;
import java.util.List;

import org.springframework.xd.tuple.Tuple;
import org.springframework.xd.tuple.TupleBuilder;

/**
 * @author Thomas Darimont
 */
public class DummyAnalytic implements Analytic<Tuple, Tuple> {

	@Override
	public Tuple evaluate(Tuple input) {

		List<String> fieldNames = new ArrayList<String>(input.getFieldNames());
		List<Object> values = new ArrayList<Object>(input.getValues());

		fieldNames.add("k3");
		values.add("v3");

		Tuple output = TupleBuilder.tuple().ofNamesAndValues(fieldNames, values);

		return output;
	}
}
