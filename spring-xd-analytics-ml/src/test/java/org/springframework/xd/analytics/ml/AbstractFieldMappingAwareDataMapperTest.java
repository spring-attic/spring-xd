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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Map;

import org.junit.Test;

/**
 * @author Thomas Darimont
 */
public class AbstractFieldMappingAwareDataMapperTest {

	AbstractFieldMappingAwareDataMapper mapperWithDefaultSplitToken = new AbstractFieldMappingAwareDataMapper() {
	};

	AbstractFieldMappingAwareDataMapper mapperWithCustomSplitToken = new AbstractFieldMappingAwareDataMapper("->") {
	};

	@Test
	public void testExtractMixedFieldNameMappingFromWithDefaultSplitToken() throws Exception {

		Map<String, String> mapping = mapperWithDefaultSplitToken.extractFieldNameMappingFrom(Arrays.asList("foo:bar", "mambo", "bubu:gaga", "salsa"));

		assertThat(mapping.size(),is(4));
		assertThat(mapping.get("foo"), is("bar"));
		assertThat(mapping.get("bubu"), is("gaga"));
		assertThat(mapping.get("mambo"), is("mambo"));
		assertThat(mapping.get("salsa"), is("salsa"));
	}


	@Test
	public void testExtractFieldNameMappingFromWithDefaultSplitToken() throws Exception {

		Map<String, String> mapping = mapperWithDefaultSplitToken.extractFieldNameMappingFrom(Arrays.asList("foo:bar", "bubu:gaga"));

		assertThat(mapping.size(),is(2));
		assertThat(mapping.get("foo"), is("bar"));
		assertThat(mapping.get("bubu"), is("gaga"));
	}

	@Test
	public void testExtractFieldNameMappingFromWithCustomSplitToken() throws Exception {

		Map<String, String> mapping = mapperWithCustomSplitToken.extractFieldNameMappingFrom(Arrays.asList("foo->bar", "bubu->gaga"));

		assertThat(mapping.size(),is(2));
		assertThat(mapping.get("foo"), is("bar"));
		assertThat(mapping.get("bubu"), is("gaga"));
	}
}
