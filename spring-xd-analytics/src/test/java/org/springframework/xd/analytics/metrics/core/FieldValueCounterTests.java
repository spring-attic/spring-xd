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

package org.springframework.xd.analytics.metrics.core;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

import org.junit.Test;

public class FieldValueCounterTests {

	@Test
	public void nameWithData() {
		Map<String, Double> map = new HashMap<String, Double>();
		map.put("obamavotes", 100.0);
		map.put("biebervotes", 100000.0);
		FieldValueCounter c = new FieldValueCounter("myFieldValueCounter", map);
		assertThat(c.getName(), equalTo("myFieldValueCounter"));
		assertThat(c.getFieldValueCount(), equalTo(map));
	}

	@Test
	public void equalsContract() {
		EqualsVerifier.forClass(FieldValueCounter.class).suppress(Warning.NONFINAL_FIELDS).verify();
	}
}
