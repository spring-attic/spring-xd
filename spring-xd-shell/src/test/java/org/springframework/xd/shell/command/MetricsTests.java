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

import java.io.File;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Test;
import org.springframework.shell.support.util.FileUtils;
import org.springframework.xd.shell.util.Table;
import org.springframework.xd.shell.util.TableHeader;

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

	@Test
	public void testRichGaugeList() throws Exception {
		createTestStream(MetricType.RICH_GAUGE);
		httpPostData(DEFAULT_HTTP_URL, "15");
		richGauge().verifyDefaultExists();
	}

	@Test
	public void testRichGaugeDisplay() throws Exception {
		createTestStream(MetricType.RICH_GAUGE);
		httpPostData(DEFAULT_HTTP_URL, "5");
		httpPostData(DEFAULT_HTTP_URL, "10");
		httpPostData(DEFAULT_HTTP_URL, "15");
		Table t = constructRichGaugeDisplay(15d, -1d, 10d, 15d, 5d, 3l);
		richGauge().verifyRichGauge(t.toString());
	}

	@Test
	public void testRichGaugeDelete() throws Exception {
		createTestStream(MetricType.RICH_GAUGE);
		httpPostData(DEFAULT_HTTP_URL, "10");
		richGauge().deleteDefaultRichGauge();
	}

	@Test
	public void testFieldValueCounterList() throws Exception {
		FileSource fileSource = newFileSource(new File(FILE_SOURCE_INPUT_DIR + TEST_STREAM_NAME));
		createFileSourceFVCStream(fileSource, "fromUser");
		copyTweetFiles(fileSource);
		fvc().verifyDefaultExists();
	}

	@Test
	public void testFieldValueCounterDisplay() throws Exception {
		TreeMap<String, Double> fvcMap = new TreeMap<String, Double>();
		fvcMap.put("BestNoSQL", 1d);
		fvcMap.put("SpringSource", 2d);
		FileSource fileSource = newFileSource(new File(FILE_SOURCE_INPUT_DIR + TEST_STREAM_NAME));
		createFileSourceFVCStream(fileSource, "fromUser");
		copyTweetFiles(fileSource);
		// Wait for some more time to allow the tweets get processed.
		Thread.sleep(10000);
		Table t = constructFVCDisplay(fvcMap);
		fvc().verifyFVCounter(t.toString());
	}

	@Test
	public void testFieldValueCounterDelete() throws Exception {
		FileSource fileSource = newFileSource(new File(FILE_SOURCE_INPUT_DIR + TEST_STREAM_NAME));
		createFileSourceFVCStream(fileSource, "fromUser");
		copyTweetFiles(fileSource);
		fvc().deleteDefaultFVCounter();
	}

	private Table constructRichGaugeDisplay(double value, double alpha, double average, double max, double min,
			long count) {
		Table t = new Table();
		NumberFormat pattern = new DecimalFormat();
		t.addHeader(1, new TableHeader(String.format("Name"))).addHeader(2, new TableHeader(DEFAULT_METRIC_NAME));
		t.newRow().addValue(1, "value").addValue(2, pattern.format(value));
		t.newRow().addValue(1, "alpha").addValue(2, pattern.format(alpha));
		t.newRow().addValue(1, "average").addValue(2, pattern.format(average));
		t.newRow().addValue(1, "max").addValue(2, pattern.format(max));
		t.newRow().addValue(1, "min").addValue(2, pattern.format(min));
		t.newRow().addValue(1, "count").addValue(2, pattern.format(count));
		return t;
	}

	private Table constructFVCDisplay(TreeMap<String, Double> fvcMap) {
		Table t = new Table();
		NumberFormat pattern = new DecimalFormat();
		t.addHeader(1, new TableHeader("FieldName=" + DEFAULT_METRIC_NAME)).addHeader(2, new TableHeader("")).addHeader(
				3, new TableHeader(""));
		t.newRow().addValue(1, "VALUE").addValue(2, "-").addValue(3, "COUNT");
		for (Map.Entry<String, Double> entry : fvcMap.descendingMap().entrySet()) {
			t.newRow().addValue(1, entry.getKey()).addValue(2, "|").addValue(3, pattern.format(entry.getValue()));
		}
		return t;
	}

	private void createTestStream(MetricType metricType) throws Exception {
		stream().create(TEST_STREAM_NAME, "http --port=%s | %s --name=%s", DEFAULT_HTTP_PORT, metricType.getName(),
				DEFAULT_METRIC_NAME);
		Thread.sleep(5000);
	}

	private void createFileSourceFVCStream(FileSource fileSource, String fieldName) throws Exception {
		stream().create(TEST_STREAM_NAME, fileSource + " | field-value-counter --fieldName=%s --counterName=%s",
				fieldName, DEFAULT_METRIC_NAME);
		Thread.sleep(5000);
	}

	private void copyTweetFiles(FileSource fileSource) throws Exception {
		URL url = this.getClass().getClassLoader().getResource(TWEETS_DIR);
		File file = new File(url.toURI());
		FileUtils.copyRecursively(file, fileSource.getFile(), false);
		Thread.sleep(5000);
	}
}