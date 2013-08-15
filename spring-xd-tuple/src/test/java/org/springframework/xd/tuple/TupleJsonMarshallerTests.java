/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.xd.tuple;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * @author David Turanski
 *
 */
public class TupleJsonMarshallerTests extends AbstractTupleMarshallerTests {

	/* (non-Javadoc)
	 * @see org.springframework.xd.tuple.AbstractTupleMarshallerTests#getMarshaller()
	 */
	@Override
	protected TupleStringMarshaller getMarshaller() {
		return new TupleJsonMarshaller();
	}

	@Test
	public void testComplexJson() throws IOException {
		Resource jsonFile = new ClassPathResource("/tweet.json");
		assertTrue(jsonFile.exists());

		BufferedReader reader = new BufferedReader(new InputStreamReader(jsonFile.getInputStream()));
		String line = null;
		StringBuilder stringBuilder = new StringBuilder();
		String ls = System.getProperty("line.separator");

		while ((line = reader.readLine()) != null) {
			stringBuilder.append(line);
			stringBuilder.append(ls);
		}
		reader.close();

		String source = stringBuilder.toString();
		TupleStringMarshaller marshaller = getMarshaller();
		Tuple tuple = marshaller.toTuple(source);
		assertEquals("Gabriel", tuple.getTuple("user").getString("name"));
		List<?> mentions = (List<?>) tuple.getTuple("entities").getValue("user_mentions");
		assertEquals(2, mentions.size());
		assertTrue(mentions.get(0) instanceof Tuple);
		Tuple t = (Tuple) mentions.get(0);
		assertEquals("someoneFollowed", t.getString("screen_name"));
	}
}
