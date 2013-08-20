/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.tuple.integration;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import org.springframework.xd.tuple.Tuple;

public class MapToTupleTransformerTests {

	@Test
	public void testTranformer() {
		MapToTupleTransformer transformer = new MapToTupleTransformer();
		Map<Object, Object> map = new HashMap<Object, Object>();
		map.put("one", 1);
		map.put("two", 2);
		map.put("three", "tres");

		Tuple tuple = transformer.transformPayload(map);
		assertThat(tuple.size(), equalTo(3));
		assertThat(tuple.getValue("one", Integer.class), equalTo(1));
		assertThat(tuple.getValue("two", Integer.class), equalTo(2));
		assertThat(tuple.getString("three"), equalTo("tres"));

	}
}
