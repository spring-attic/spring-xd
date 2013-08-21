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

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import org.springframework.xd.tuple.Tuple;

/**
 * Unit test for JsonToTupleTransformer.
 * 
 * @author Mark Fisher
 */
public class JsonToTupleTransformerTests {

	@Test
	public void testTranformer() throws Exception {
		JsonToTupleTransformer transformer = new JsonToTupleTransformer();
		String json = "{\"one\":1, \"two\":2, \"three\":\"tres\"}";
		Tuple tuple = transformer.transformPayload(json);
		assertThat(tuple.size(), equalTo(3));
		assertThat(tuple.getValue("one", Integer.class), equalTo(1));
		assertThat(tuple.getValue("two", Integer.class), equalTo(2));
		assertThat(tuple.getString("three"), equalTo("tres"));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void numberArray() throws Exception {
		JsonToTupleTransformer transformer = new JsonToTupleTransformer();
		String json = "{\"numbers\":[1,2,3]}";
		Tuple tuple = transformer.transformPayload(json);
		assertThat(tuple.size(), equalTo(1));
		List<Integer> list = Arrays.asList(1, 2, 3);
		List<Integer> actual = tuple.getValue("numbers", List.class);
		assertThat(actual, equalTo(list));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void stringArray() throws Exception {
		JsonToTupleTransformer transformer = new JsonToTupleTransformer();
		String json = "{\"strings\":[\"one\",\"two\",\"three\"]}";
		Tuple tuple = transformer.transformPayload(json);
		assertThat(tuple.size(), equalTo(1));
		List<String> list = Arrays.asList("one", "two", "three");
		List<String> actual = tuple.getValue("strings", List.class);
		assertThat(actual, equalTo(list));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void mixedArray() throws Exception {
		JsonToTupleTransformer transformer = new JsonToTupleTransformer();
		String json = "{\"stuff\":[1,\"two\",false]}";
		Tuple tuple = transformer.transformPayload(json);
		assertThat(tuple.size(), equalTo(1));
		List<Object> list = Arrays.<Object> asList(1, "two", false);
		List<Object> actual = tuple.getValue("stuff", List.class);
		assertThat(actual, equalTo(list));
	}
}
