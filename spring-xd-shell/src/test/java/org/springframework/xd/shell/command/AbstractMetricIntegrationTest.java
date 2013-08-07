/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.xd.shell.command;

import org.junit.After;
import org.springframework.shell.core.CommandResult;
import org.springframework.xd.shell.AbstractShellIntegrationTest;
import org.springframework.xd.shell.util.Table;
import org.springframework.xd.shell.util.TableRow;

import static org.junit.Assert.*;

/**
 * Abstract class to hold all metrics related helper methods.
 * 
 * @author Ilayaperumal Gopinathan
 * 
 */
public abstract class AbstractMetricIntegrationTest extends AbstractShellIntegrationTest {

	// These two are used across the tests, hopefully 9193 is free on most dev boxes and
	// CI servers
	public static final String DEFAULT_HTTP_PORT = "9193";

	public static final String DEFAULT_HTTP_URL = "http://localhost:" + DEFAULT_HTTP_PORT;

	protected static final String DEFAULT_METRIC_NAME = "bar";

	@After
	public void deleteMetrics() {
		for (TestMetricType metricType : TestMetricType.values()) {
			if (isMetricAvailable(DEFAULT_METRIC_NAME, metricType))
				executeMetricDelete(DEFAULT_METRIC_NAME, metricType);
		}
	}

	/**
	 * Check if the metric exists
	 * 
	 * @param metricName
	 * @param metricType
	 * @return boolean
	 */
	private boolean isMetricAvailable(String metricName, TestMetricType metricType) {
		Table t = (Table) getShell().executeCommand(metricType.getName() + " list").getResult();
		return t.getRows().contains(new TableRow().addValue(1, metricName));
	}

	/**
	 * Verify and assert if the metric exists in the metrics list
	 * 
	 * @param metricName the metric name to check
	 */
	protected void checkIfMetricExists(String metricName, TestMetricType metricType) {
		Table t = (Table) getShell().executeCommand(metricType.getName() + " list").getResult();
		assertTrue("Failure. " + metricType.getName() + " '" + metricName + "' doesn't exist",
				t.getRows().contains(new TableRow().addValue(1, metricName)));
	}

	/**
	 * Check the metric value using "display" shell command.
	 */
	protected void checkMetricValue(String metricName, TestMetricType metricType, String expectedValue) {
		CommandResult cr = executeCommand(metricType.getName() + " display --name " + metricName);
		assertEquals(expectedValue, cr.getResult());
	}

	/**
	 * Delete the metric with the given name.
	 */
	protected void executeMetricDelete(String metricName, TestMetricType metricType) {
		CommandResult cr = executeCommand(metricType.getName() + " delete --name " + metricName);
		assertEquals("Deleted " + metricType.getName() + " '" + metricName + "'", cr.getResult());
	}

	protected void verifyCounter(String expectedValue) {
		verifyCounter(DEFAULT_METRIC_NAME, expectedValue);
	}

	protected void verifyCounter(String counterName, String expectedValue) {
		checkIfMetricExists(counterName, TestMetricType.COUNTER);
		checkMetricValue(counterName, TestMetricType.COUNTER, expectedValue);
	}

	/**
	 * Deletes the given counter
	 * 
	 * @param counterName
	 */
	protected void deleteCounter(String counterName) {
		executeMetricDelete(counterName, TestMetricType.COUNTER);
	}

	protected enum TestMetricType {
		COUNTER("counter"), AGGR_COUNTER("aggregatecounter"), GAUGE("gauge"), RICH_GAUGE("richgauge");

		private String name;

		private TestMetricType(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}
	};

}