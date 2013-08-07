/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.xd.shell.command;

import org.junit.Test;

/**
 * Tests various metrics related sinks.
 * 
 * @author Eric Bottard
 * @author Ilayaperumal Gopinathan
 */
public class MetricsTests extends AbstractStreamIntegrationTest {

	private static final String TEST_STREAM_NAME = "foo";

	@Test
	public void testSimpleCounter() throws Exception {
		createTestStream(TestMetricType.COUNTER.getName());
		httpPostData(DEFAULT_HTTP_URL, "one");
		httpPostData(DEFAULT_HTTP_URL, "one");
		httpPostData(DEFAULT_HTTP_URL, "two");
		verifyCounter("3");
	}

	@Test
	public void testSimpleCounterImplicitName() throws Exception {
		String streamName = "foo";
		executeStreamCreate(streamName, "http --port=" + DEFAULT_HTTP_PORT + " | counter");
		Thread.sleep(5000);
		httpPostData(DEFAULT_HTTP_URL, "one");
		verifyCounter(streamName, "1");
		// Explicitly delete the counter
		executeMetricDelete(streamName, TestMetricType.COUNTER);
	}

	@Test
	public void testCounterDeletion() throws Exception {
		createTestStream(TestMetricType.COUNTER.getName());
		httpPostData(DEFAULT_HTTP_URL, "one");
		executeMetricDelete(DEFAULT_METRIC_NAME, TestMetricType.COUNTER);
	}

	@Test
	public void testAggregateCounterList() throws Exception {
		createTestStream(TestMetricType.AGGR_COUNTER.getName());
		httpPostData(DEFAULT_HTTP_URL, "one");
		checkIfMetricExists(DEFAULT_METRIC_NAME, TestMetricType.AGGR_COUNTER);
	}

	@Test
	public void testAggregateCounterDelete() throws Exception {
		createTestStream(TestMetricType.AGGR_COUNTER.getName());
		httpPostData(DEFAULT_HTTP_URL, "one");
		executeMetricDelete(DEFAULT_METRIC_NAME, TestMetricType.AGGR_COUNTER);
	}

	private void createTestStream(String metricType) throws Exception {
		executeStreamCreate(TEST_STREAM_NAME, "http --port=" + DEFAULT_HTTP_PORT + " | " + metricType + " --name="
				+ DEFAULT_METRIC_NAME);
		Thread.sleep(5000);
	}

}