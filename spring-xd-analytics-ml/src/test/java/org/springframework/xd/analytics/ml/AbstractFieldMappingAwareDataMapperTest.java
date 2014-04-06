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
