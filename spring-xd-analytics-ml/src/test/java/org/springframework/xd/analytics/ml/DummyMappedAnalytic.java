
package org.springframework.xd.analytics.ml;

import java.util.HashMap;
import java.util.Map;

import org.springframework.xd.tuple.Tuple;

/**
 * @author Thomas Darimont
 */
public class DummyMappedAnalytic extends
		MappedAnalytic<Tuple, Tuple, Map<String, String>, Map<String, String>, DummyMappedAnalytic> {

	/**
	 * Creates a new {@link org.springframework.xd.analytics.ml.MappedAnalytic}.
	 * 
	 * @param inputMapper must not be {@literal null}.
	 * @param outputMapper must not be {@literal null}.
	 */
	public DummyMappedAnalytic(InputMapper<Tuple, DummyMappedAnalytic, Map<String, String>> inputMapper,
			OutputMapper<Tuple, Tuple, DummyMappedAnalytic, Map<String, String>> outputMapper) {
		super(inputMapper, outputMapper);
	}

	@Override
	protected Map<String, String> evaluateInternal(Map<String, String> modelInput) {

		Map<String, String> output = new HashMap<String, String>();
		for (Map.Entry<String, String> entry : modelInput.entrySet()) {
			output.put(entry.getKey(), entry.getValue().toUpperCase());
		}

		return output;
	}
}
