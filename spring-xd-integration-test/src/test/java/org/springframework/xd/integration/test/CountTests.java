/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.xd.integration.test;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

/**
 * Tests the Count Metrics.
 */
public class CountTests extends AbstractIntegrationTest {

	private String streamName;

	private static final String GOOD_BAD_TEXT = "{\"id\":A123,\"entities\":{\"hashtags\"" +
			":[{\"text\":\"good\"},{\"text\":\"bad\"}],\"urls\":[]}}";
	private static final String GOOD_TEXT = "{\"id\":B123,\"entities\":{\"hashtags\"" +
			":[{\"text\":\"good\"},{\"text\":\"good\"}],\"urls\":[]}}";
	private static final String BAD_TEXT = "{\"id\":C123, \"entities\":{\"hashtags\"" +
			":[{\"text\":\"bad\"},{\"text\":\"bad\"}],\"urls\":[]}}'";


	@Before
	public void initialize() {
		streamName = "scis" + UUID.randomUUID().toString();
	}

	/**
	 * Verifies that a single count can be captured from a stream.
	 */
	@Test
	public void testSingleCountInStream() {
		String counterName = "cn" + UUID.randomUUID().toString();
		String labelName = "ln" + UUID.randomUUID().toString();

		stream(streamName, sources.http() + XD_DELIMITER + labelName +
				": filter --expression=#jsonPath(payload,'$.entities.hashtags[*]." +
				"text').contains('good')" + XD_DELIMITER +
				sinks.file());

		stream(sources.tap(streamName).label(labelName) + XD_TAP_DELIMITER +
				sinks.counterSink(counterName));

		sources.httpSource(streamName).postData(GOOD_TEXT);
		waitForMetric(counterName);

		assertEquals("The value expected for this counter was 1", 1, getCount(counterName));
	}

	/**
	 * Verifies that we can capture multiple counts from the same stream.
	 */
	@Test
	public void testMultiCountsInStream() {
		String afterGoodCounterName = "aftergoodcn" + UUID.randomUUID().toString();
		String goodAndBadCounterName = "goodandbadcn" + UUID.randomUUID().toString();

		String afterGoodLabelName = "aftergood";
		String goodAndBadLabelName = "goodandbad";

		stream(streamName, sources.http() + XD_DELIMITER +
				" good: filter --expression=#jsonPath(payload,'$.entities.hashtags[*]." +
				"text').contains('good') " + XD_DELIMITER +
				afterGoodLabelName + ": filter --expression=true " + XD_DELIMITER +
				"bad: filter --expression=#jsonPath(payload,'$." +
				"entities.hashtags[*].text').contains('bad') " + XD_DELIMITER +
				goodAndBadLabelName + ": splitter --expression=#jsonPath(payload,'$.id') "
				+ XD_DELIMITER + sinks.file());

		stream("tpaftergood", sources.tap(streamName).label(afterGoodLabelName) +
				XD_TAP_DELIMITER + sinks.counterSink(afterGoodCounterName));
		stream("tpgoodbad", sources.tap(streamName).
				label(goodAndBadLabelName) + XD_TAP_DELIMITER +
				sinks.counterSink(goodAndBadCounterName));

		sources.httpSource(streamName).postData(GOOD_TEXT);
		sources.httpSource(streamName).postData(GOOD_BAD_TEXT);
		sources.httpSource(streamName).postData(BAD_TEXT);

		waitForMetric(afterGoodCounterName);
		waitForMetric(goodAndBadCounterName);

		assertEquals("The value expected for the  aftergood filter counter was 2", 2,
				getCount(afterGoodCounterName));

		assertEquals("The value expected for the goodandbad filter counter was 1", 1,
				getCount(goodAndBadCounterName));

	}
}
