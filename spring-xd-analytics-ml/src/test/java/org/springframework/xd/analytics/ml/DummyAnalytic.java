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
