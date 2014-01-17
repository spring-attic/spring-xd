/*
 * Copyright 2013 the original author or authors.
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
import static org.springframework.xd.shell.command.fixtures.XDMatchers.hasValue;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import org.springframework.util.StopWatch;
import org.springframework.xd.shell.command.fixtures.CounterSink;
import org.springframework.xd.shell.command.fixtures.HttpSource;


/**
 * 
 * @author Eric Bottard
 */
@RunWith(Parameterized.class)
public class MessageStoreBenchmarkTests extends AbstractStreamIntegrationTest {

	@Parameters(name = "{0}-{1}")
	public static Iterable<Object[]> roots() {
		List<Object[]> result = new ArrayList<Object[]>();
		int runs = 5;

		for (int i = 1; i <= runs; i++) {
			Random random = new Random();
			String dbname = "db" + random.nextInt(Integer.MAX_VALUE);
			result.add(new Object[] { "jdbc", i, String.format(
					" --driverClass=org.hsqldb.jdbc.JDBCDriver --url=jdbc:hsqldb:file:/tmp/%s --initdb=hsqldb", dbname) });
			result.add(new Object[] { "memory", i, "" });
			result.add(new Object[] { "redis", i, "" });
		}
		return result;
	}

	private String streamDynamicPart;

	private String storeName;

	public MessageStoreBenchmarkTests(String storeName, int run, String streamDynamicPart) {
		this.storeName = storeName;
		this.streamDynamicPart = streamDynamicPart;
	}

	@Test
	@Ignore
	public void benchmark() throws Exception {
		HttpSource source = newHttpSource();
		CounterSink sink = metrics().newCounterSink();
		stream().create(
				storeName,
				"%s | aggregator --store=%s --count=3 --aggregation=T(org.springframework.util.StringUtils).collectionToDelimitedString(#this.![payload],' ') --timeout=500000%s | %s ",
				source, storeName,
				streamDynamicPart, sink);

		source.ensureReady();

		StopWatch stopWatch = new StopWatch(storeName);
		stopWatch.start();
		final int howMany = 1000;
		for (int i = 0; i < 3 * howMany; i++) {
			source.postData("boo");
		}

		assertThat(sink, eventually(hasValue(NumberFormat.getInstance().format(howMany))));

		stopWatch.stop();
		System.out.println(stopWatch.prettyPrint());

	}

}
