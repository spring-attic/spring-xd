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

import org.springframework.shell.core.CommandResult;
import org.springframework.shell.core.JLineShellComponent;
import org.springframework.xd.shell.util.Table;
import org.springframework.xd.shell.util.TableRow;

import static org.junit.Assert.*;

/**
 * 
 * @author mpollack
 */
public class MetricCommandTemplate extends AbstractCommandTemplate {

	public MetricCommandTemplate(JLineShellComponent shell) {
		super(shell);
	}

	/**
	 * Check if the metric exists
	 * 
	 * @param metricName
	 * @param metricType
	 * @return boolean
	 */
	protected boolean isMetricAvailable(String metricName, MetricType metricType) {
		Table t = (Table) getShell().executeCommand(metricType.getName() + " list").getResult();
		return t.getRows().contains(new TableRow().addValue(1, metricName));
	}

	/**
	 * Verify and assert if the metric exists in the metrics list
	 * 
	 * @param metricName the metric name to check
	 */
	protected void checkIfMetricExists(String metricName, MetricType metricType) {
		Table t = (Table) getShell().executeCommand(metricType.getName() + " list").getResult();
		assertTrue("Failure. " + metricType.getName() + " '" + metricName + "' doesn't exist",
				t.getRows().contains(new TableRow().addValue(1, metricName)));
	}

	/**
	 * Check the metric value using "display" shell command.
	 */
	protected void checkMetricValue(String metricName, MetricType metricType, String expectedValue) {
		CommandResult cr = executeCommand(metricType.getName() + " display --name " + metricName);
		assertEquals(expectedValue, cr.getResult());
	}

	/**
	 * Delete the metric with the given name.
	 */
	protected void executeMetricDelete(String metricName, MetricType metricType) {
		CommandResult cr = executeCommand(metricType.getName() + " delete --name " + metricName);
		assertEquals("Deleted " + metricType.getName() + " '" + metricName + "'", cr.getResult());
	}

	public void destroyDefaultNamedMetrics() {
		for (MetricType metricType : MetricType.values()) {
			if (isMetricAvailable(DEFAULT_METRIC_NAME, metricType))
				executeMetricDelete(DEFAULT_METRIC_NAME, metricType);
		}
	}

}
