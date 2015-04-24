/*
 * Copyright 2011-2014 the original author or authors.
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

package org.springframework.xd.integration.test;

import org.junit.Test;
import java.util.UUID;


/**
 * Verifies that processors are functional on a XD Cluster Instance.
 *
 * @author Glenn Renfro
 */
public class ProcessorTest extends AbstractIntegrationTest {


	private final static String HTTP_MODULE_NAME = STREAM_NAME+".source.http.1";

	private final static String FILTER_MODULE_NAME = STREAM_NAME+".processor.filter.1";

	private final static String OUTPUT_CHANNEL_NAME = "output";

	private final static String INPUT_CHANNEL_NAME = "input";

	private final static String TO_SPEL_CHANNEL_NAME = "to.spel";

	private final static String TO_SCRIPT_CHANNEL_NAME = "to.script";

	/**
	 * Evaluates that a single data entry of "BAD" is filtered out and not stored. Verifies that the correct elements of
	 * the filter, filtered out the data.
	 *
	 */
	@Test
	public void testFailedSink() {
		String filterContent = "BAD";
		stream(sources.http() + XD_DELIMITER + " filter --expression=payload=='good' " + XD_DELIMITER
				+ sinks.file());
		sources.httpSource().postData(filterContent);
		waitForXD();
		assertReceivedByProcessor(FILTER_MODULE_NAME, INPUT_CHANNEL_NAME, 1);
		assertReceivedByProcessor(FILTER_MODULE_NAME, OUTPUT_CHANNEL_NAME, 0);
		assertReceivedByProcessor(FILTER_MODULE_NAME, TO_SPEL_CHANNEL_NAME, 1);
		assertReceivedByProcessor(FILTER_MODULE_NAME, TO_SCRIPT_CHANNEL_NAME, 0);
		assertReceivedBySource(HTTP_MODULE_NAME, OUTPUT_CHANNEL_NAME, 1);

	}

	/**
	 * Evaluates that a single data entry of "good" is not allowed past the filter and stored in a file..
	 *
	 */
	@Test
	public void testFilter() {
		String filterContent = "good";
		stream(sources.http() + XD_DELIMITER + " filter --expression=payload=='" + filterContent + "' " + XD_DELIMITER
				+ sinks.file());
		sources.httpSource().postData(filterContent);
		assertValid(filterContent, sinks.file());
		assertReceived(1);
	}

	/**
	 * Tests that a json string is written to file if it meets the filter criteria.
	 */
	@Test
	public void testJsonFilter() {
		String lastName = UUID.randomUUID().toString();
		String goodData = "{\"firstName\":\"good\", \"lastName\":\"g" + lastName + "\"}";
		String badData = "{\"firstName\":\"bad\", \"lastName\":\"b" + lastName + "\"}";

		stream(sources.http() + XD_DELIMITER
				+ " filter --expression=#jsonPath(payload,'$.firstName').contains('"
				+ "good') " + XD_DELIMITER
				+ sinks.file());

		sources.httpSource().postData(goodData);
		sources.httpSource().postData(badData);

		assertValid(goodData, sinks.file());
		assertReceived(1);
	}

	/**
	 * Tests Multiple Filters with tags in the same stream.
	 */
	@Test
	public void testMultipleFilters() {
		String idBase = UUID.randomUUID().toString();
		String goodBadText = "{\"id\":A" + idBase
				+ ",\"entities\":{\"hashtags\":[{\"text\":\"good\"},"
				+ "{\"text\":\"bad\"}],\"urls\":[]}}";
		String goodGoodText = "{\"id\":B" + idBase
				+ ",\"entities\":{\"hashtags\":[{\"text\":\"good\"},"
				+ "{\"text\":\"good\"}],\"urls\":[]}}";
		String badBadText = "{\"id\":C" + idBase
				+ ", \"entities\":{\"hashtags\":[{\"text\":\"bad\"},"
				+ "{\"text\":\"bad\"}],\"urls\":[]}}'";

		stream(sources.http() + XD_DELIMITER
				+ " good: filter --expression=#jsonPath(payload,'$.entities.hashtags[*]."
				+ "text').contains('good') "
				+ XD_DELIMITER + " aftergood: filter --expression=true "
				+ XD_DELIMITER + " bad:  filter --expression=#jsonPath(payload,'$."
				+ "entities.hashtags[*].text').contains('bad') "
				+ XD_DELIMITER
				+ " goodandbad: splitter --expression=#jsonPath(payload,'$.id') "
				+ XD_DELIMITER + sinks.file());

		sources.httpSource().postData(goodBadText);
		sources.httpSource().postData(goodGoodText);
		sources.httpSource().postData(badBadText);

		assertValid("A" + idBase, sinks.file());
		assertReceived(1);

	}


}
