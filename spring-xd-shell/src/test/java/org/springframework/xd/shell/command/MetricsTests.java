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
		createTestStream(MetricType.COUNTER);
		httpPostData(DEFAULT_HTTP_URL, "one");
		httpPostData(DEFAULT_HTTP_URL, "one");
		httpPostData(DEFAULT_HTTP_URL, "two");
		counter().verifyCounter("3");
	}

	@Test
	public void testSimpleCounterImplicitName() throws Exception {
		String streamName = "foo";
		stream().create(streamName, "http --port=%s | counter", DEFAULT_HTTP_PORT);
		Thread.sleep(5000);
		httpPostData(DEFAULT_HTTP_URL, "one");
		counter().verifyCounter(streamName, "1");
		// Explicitly delete the counter
		counter().deleteCounter(streamName);
	}

	@Test
	public void testCounterDeletion() throws Exception {
		createTestStream(MetricType.COUNTER);
		httpPostData(DEFAULT_HTTP_URL, "one");
		counter().deleteDefaultCounter();
	}

	@Test
	public void testAggregateCounterList() throws Exception {
		createTestStream(MetricType.AGGR_COUNTER);
		httpPostData(DEFAULT_HTTP_URL, "one");
		aggCounter().verifyDefaultExists();
	}

	@Test
	public void testAggregateCounterDelete() throws Exception {
		createTestStream(MetricType.AGGR_COUNTER);
		httpPostData(DEFAULT_HTTP_URL, "one");
		aggCounter().deleteDefaultCounter();
	}

	private void createTestStream(MetricType metricType) throws Exception {
		stream().create(TEST_STREAM_NAME, "http --port=%s | %s --name=%s", DEFAULT_HTTP_PORT, metricType.getName(),
				DEFAULT_METRIC_NAME);
		Thread.sleep(5000);
	}

}