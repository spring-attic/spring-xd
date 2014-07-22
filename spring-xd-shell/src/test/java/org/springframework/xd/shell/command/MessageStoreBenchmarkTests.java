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

package org.springframework.xd.shell.command;

import static org.junit.Assert.assertThat;
import static org.springframework.xd.shell.command.fixtures.XDMatchers.eventually;
import static org.springframework.xd.shell.command.fixtures.XDMatchers.hasValue;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import org.springframework.util.StopWatch;
import org.springframework.xd.shell.command.fixtures.HttpSource;
import org.springframework.xd.test.fixtures.CounterSink;


/**
 * Tests each Message Store type within the context of a simple aggregation use-case.
 * For proper benchmarking, override the values of {@link #EXECUTIONS_PER_STORE_TYPE},
 * {@link #MESSAGE_GROUPS_PER_EXECUTION}, and/or {@link #MESSAGES_PER_GROUP}.
 * The current values are minimal to avoid adding overhead to the test suite.
 *
 * @author Eric Bottard
 * @author Mark Fisher
 */
@RunWith(Parameterized.class)
public class MessageStoreBenchmarkTests extends AbstractStreamIntegrationTest {

	private static final int EXECUTIONS_PER_STORE_TYPE = 1;

	private static final int MESSAGE_GROUPS_PER_EXECUTION = 3;

	private static final int MESSAGES_PER_GROUP = 4;

	@Parameters(name = "{0}-{1}")
	public static Iterable<Object[]> roots() {
		List<Object[]> result = new ArrayList<Object[]>();
		int runs = EXECUTIONS_PER_STORE_TYPE;

		for (int i = 1; i <= runs; i++) {
			Random random = new Random();
			String dbname = "db" + random.nextInt(Integer.MAX_VALUE);
			result.add(new Object[] {
				"jdbc",
				i,
				String.format(
						" --driverClassName=org.hsqldb.jdbc.JDBCDriver --url=jdbc:hsqldb:file:/tmp/%s --initializeDatabase=true --dbkind=hsqldb --username=sa --password=''",
						dbname) });
			result.add(new Object[] { "memory", i, "" });
			result.add(new Object[] { "redis", i, "" });
		}
		return result;
	}


	private final String storeName;

	private final int executionCount;

	private final String streamDynamicPart;

	public MessageStoreBenchmarkTests(String storeName, int executionCount, String streamDynamicPart) {
		this.storeName = storeName;
		this.executionCount = executionCount;
		this.streamDynamicPart = streamDynamicPart;
	}

	@Test
	public void benchmark() throws Exception {
		HttpSource source = newHttpSource();
		CounterSink sink = metrics().newCounterSink();
		stream().create(
				generateStreamName(),
				"%s | aggregator --store=%s --count=%d --aggregation=T(org.springframework.util.StringUtils).collectionToDelimitedString(#this.![payload],' ') --timeout=500000%s | %s ",
				source, storeName, MESSAGES_PER_GROUP,
				streamDynamicPart, sink);

		source.ensureReady();

		StopWatch stopWatch = new StopWatch(storeName);
		stopWatch.start(storeName + "-" + executionCount);
		for (int i = 0; i < MESSAGES_PER_GROUP * MESSAGE_GROUPS_PER_EXECUTION; i++) {
			source.postData("boo");
		}

		assertThat(sink, eventually(hasValue(NumberFormat.getInstance().format(MESSAGE_GROUPS_PER_EXECUTION))));

		stopWatch.stop();
		System.out.println(stopWatch.prettyPrint());
	}

}
