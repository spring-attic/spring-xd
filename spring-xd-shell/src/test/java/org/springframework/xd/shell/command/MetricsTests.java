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

import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Test;

import org.springframework.util.StreamUtils;
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

	private HttpSource httpSource;

	@Test
	public void testSimpleCounter() throws Exception {
		createTestStream(MetricType.COUNTER);
		httpSource.postData("one");
		httpSource.postData("one");
		httpSource.postData("two");
		counter().verifyCounter("3");
	}

	@Test
	public void testSimpleCounterImplicitName() throws Exception {
		String streamName = "foo";
		httpSource = newHttpSource();
		stream().create(streamName, "%s | counter", httpSource);
		httpSource.postData("one");
		counter().verifyCounter(streamName, "1");
		// Explicitly delete the counter
		counter().deleteCounter(streamName);
	}

	@Test
	public void testCounterDeletion() throws Exception {
		createTestStream(MetricType.COUNTER);
		httpSource.postData("one");
		counter().deleteDefaultCounter();
	}

	@Test
	public void testAggregateCounterList() throws Exception {
		createTestStream(MetricType.AGGR_COUNTER);
		httpSource.postData("one");
		aggCounter().verifyDefaultExists();
	}

	@Test
	public void testAggregateCounterDelete() throws Exception {
		createTestStream(MetricType.AGGR_COUNTER);
		httpSource.postData("one");
		aggCounter().deleteDefaultCounter();
	}

	@Test
	public void testRichGaugeList() throws Exception {
		createTestStream(MetricType.RICH_GAUGE);
		httpSource.postData("15");
		richGauge().verifyDefaultExists();
	}

	@Test
	public void testRichGaugeDisplay() throws Exception {
		createTestStream(MetricType.RICH_GAUGE);
		httpSource.postData("5");
		httpSource.postData("10");
		httpSource.postData("15");
		Table t = constructRichGaugeDisplay(15d, -1d, 10d, 15d, 5d, 3L);
		richGauge().verifyRichGauge(t.toString());
	}

	@Test
	public void testRichGaugeDelete() throws Exception {
		createTestStream(MetricType.RICH_GAUGE);
		httpSource.postData("10");
		richGauge().deleteDefaultRichGauge();
	}

	@Test
	public void testFieldValueCounterList() throws Exception {
		TailSource tailSource = newTailSource();
		tailTweets(tailSource);
		createTailSourceFVCStream(tailSource, "fromUser");
		Thread.sleep(3000);
		fvc().verifyDefaultExists();
	}

	@Test
	public void testFieldValueCounterDisplay() throws Exception {
		TreeMap<String, Double> fvcMap = new TreeMap<String, Double>();
		fvcMap.put("BestNoSQL", 1d);
		fvcMap.put("SpringSource", 2d);
		TailSource tailSource = newTailSource();
		tailTweets(tailSource);
		createTailSourceFVCStream(tailSource, "fromUser");
		Thread.sleep(2000);
		Table t = constructFVCDisplay(fvcMap);
		fvc().verifyFVCounter(t.toString());
	}

	@Test
	public void testFieldValueCounterDelete() throws Exception {
		TailSource tailSource = newTailSource();
		createTailSourceFVCStream(tailSource, "fromUser");
		tailTweets(tailSource);
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
		httpSource = newHttpSource(9193);
		stream().create(TEST_STREAM_NAME, "%s | %s --name=%s", httpSource, metricType.getName(), DEFAULT_METRIC_NAME);
		httpSource.ensureReady();
	}

	private void createTailSourceFVCStream(TailSource tailSource, String fieldName) throws Exception {
		stream().create(TEST_STREAM_NAME, tailSource + " | field-value-counter --fieldName=%s --counterName=%s",
				fieldName, DEFAULT_METRIC_NAME);
	}

	private void tailTweets(TailSource tailSource) throws Exception {
		for (int i = 1; i <= 3; i++) {
			String tweet = StreamUtils.copyToString(getClass().getResourceAsStream("/tweet" + i + ".txt"),
					Charset.forName("UTF-8"));
			tailSource.appendToFile(tweet);
		}
	}
}
