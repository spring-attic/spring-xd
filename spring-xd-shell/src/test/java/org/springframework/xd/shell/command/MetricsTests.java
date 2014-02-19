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

import static org.junit.Assert.assertThat;
import static org.springframework.xd.shell.command.fixtures.XDMatchers.eventually;
import static org.springframework.xd.shell.command.fixtures.XDMatchers.exists;
import static org.springframework.xd.shell.command.fixtures.XDMatchers.hasValue;

import java.nio.charset.Charset;
import java.util.TreeMap;

import org.junit.Test;

import org.springframework.util.StreamUtils;
import org.springframework.xd.shell.command.fixtures.AggregateCounterSink;
import org.springframework.xd.shell.command.fixtures.CounterSink;
import org.springframework.xd.shell.command.fixtures.FieldValueCounterSink;
import org.springframework.xd.shell.command.fixtures.HttpSource;
import org.springframework.xd.shell.command.fixtures.RichGaugeSink;
import org.springframework.xd.shell.command.fixtures.TailSource;
import org.springframework.xd.shell.util.Table;


/**
 * Tests various metrics related sinks.
 * 
 * @author Eric Bottard
 * @author Ilayaperumal Gopinathan
 */
public class MetricsTests extends AbstractStreamIntegrationTest {

	private HttpSource httpSource;

	@Test
	public void testSimpleCounter() throws Exception {
		httpSource = newHttpSource();
		CounterSink counter = metrics().newCounterSink();
		stream().create(generateStreamName(), "%s | %s", httpSource, counter);

		httpSource.postData("one");
		httpSource.postData("one");
		httpSource.postData("two");

		assertThat(counter, eventually(exists()));
		assertThat(counter, eventually(hasValue("3")));
	}

	@Test
	public void testSimpleCounterImplicitName() throws Exception {
		String streamName = generateStreamName();
		httpSource = newHttpSource();

		// Create counter object, but don't use its toString
		// in stream def. Instead, we know it should be named like
		// the streamname.
		CounterSink counter = metrics().newCounterSink(streamName);
		stream().create(streamName, "%s | counter", httpSource);
		httpSource.postData("one");
		assertThat(counter, eventually(hasValue("1")));
	}

	@Test
	public void testAggregateCounterList() throws Exception {
		httpSource = newHttpSource();
		AggregateCounterSink counter = metrics().newAggregateCounterSink();
		stream().create(generateStreamName(), "%s | %s", httpSource, counter);

		httpSource.postData("one");
		httpSource.postData("one");
		httpSource.postData("two");

		assertThat(counter, eventually(exists()));
	}

	@Test
	public void testAggregateCounterImplicitName() throws Exception {
		String streamName = generateStreamName();
		httpSource = newHttpSource();

		// Create sink object, but don't use its toString
		// in stream def. Instead, we know it should be named like
		// the streamname.
		AggregateCounterSink counter = metrics().newAggregateCounterSink(streamName);
		stream().create(streamName, "%s | aggregatecounter", httpSource);
		httpSource.postData("one");
		assertThat(counter, eventually(exists()));
	}


	@Test
	public void testRichGaugeDisplay() throws Exception {
		httpSource = newHttpSource();
		RichGaugeSink sink = metrics().newRichGauge();
		stream().create(generateStreamName(), "%s | %s", httpSource, sink);

		httpSource.ensureReady();
		httpSource.postData("5");
		httpSource.postData("10");
		httpSource.postData("15");
		Table t = sink.constructRichGaugeDisplay(15d, -1d, 10d, 15d, 5d, 3L);

		assertThat(sink, eventually(exists()));
		assertThat(sink, eventually(hasValue(t)));
	}

	@Test
	public void testFieldValueCounterList() throws Exception {
		TailSource tailSource = newTailSource();
		tailTweets(tailSource);

		FieldValueCounterSink sink = metrics().newFieldValueCounterSink("fromUser");

		stream().create(generateStreamName(), "%s | %s", tailSource, sink);
		assertThat(sink, eventually(exists()));
	}

	@Test
	public void testFieldValueCounterDisplay() throws Exception {
		TreeMap<String, Double> fvcMap = new TreeMap<String, Double>();
		fvcMap.put("BestNoSQL", 1d);
		fvcMap.put("SpringSource", 2d);
		TailSource tailSource = newTailSource();
		tailTweets(tailSource);

		FieldValueCounterSink sink = metrics().newFieldValueCounterSink("fromUser");
		stream().create(generateStreamName(), "%s | %s", tailSource, sink);

		Table t = sink.constructFVCDisplay(fvcMap);
		assertThat(sink, eventually(hasValue(t)));
	}

	private void tailTweets(TailSource tailSource) throws Exception {
		for (int i = 1; i <= 3; i++) {
			String tweet = StreamUtils.copyToString(getClass().getResourceAsStream("/tweet" + i + ".txt"),
					Charset.forName("UTF-8"));
			tailSource.appendToFile(tweet);
		}
	}
}
