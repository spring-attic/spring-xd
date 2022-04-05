/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
