/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.analytics.ml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.xd.tuple.Tuple;
import org.springframework.xd.tuple.TupleBuilder;

/**
 * @author Thomas Darimont
 */
public class DummyOutputMapper implements OutputMapper<Tuple, Tuple, DummyMappedAnalytic, Map<String, String>> {


	/**
	 * Ordering of fields in the resulting {@link org.springframework.xd.tuple.Tuple} can be different than the ordering
	 * of the input {@code Tuple}.
	 * 
	 * @param analytic the {@link org.springframework.xd.analytics.ml.Analytic} that can be used to retrieve mapping
	 *        information.
	 * @param input the input for this {@link org.springframework.xd.analytics.ml.Analytic} that could be used to build
	 *        the model {@code O}.
	 * @param modelOutput the raw unmapped model output {@code MO}.
	 * @return
	 */
	@Override
	public Tuple mapOutput(DummyMappedAnalytic analytic, Tuple input, Map<String, String> modelOutput) {

		List<String> fieldNames = new ArrayList<String>();
		List<Object> values = new ArrayList<Object>();

		for (Map.Entry<String, String> entry : modelOutput.entrySet()) {
			fieldNames.add(entry.getKey());
			values.add(entry.getValue());
		}

		return TupleBuilder.tuple().ofNamesAndValues(fieldNames, values);
	}

}
