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

import org.springframework.shell.core.JLineShellComponent;

/**
 * 
 * @author Mark Pollack
 */
public class CounterCommandTemplate extends AbstractCommandTemplate {

	private MetricCommandTemplate metricCommandTemplate;

	/**
	 * Construct a new CounterCommandTemplate, given a spring shell.
	 * 
	 * @param shell the spring shell to execute commands against
	 */
	public CounterCommandTemplate(JLineShellComponent shell) {
		super(shell);
		metricCommandTemplate = new MetricCommandTemplate(shell);
	}

	public void verifyCounter(String expectedValue) {
		verifyCounter(DEFAULT_METRIC_NAME, expectedValue);
	}

	public void verifyCounter(String counterName, String expectedValue) {
		metricCommandTemplate.checkIfMetricExists(counterName, MetricType.COUNTER);
		metricCommandTemplate.checkMetricValue(counterName, MetricType.COUNTER, expectedValue);
	}

	/**
	 * Deletes the given counter
	 * 
	 * @param counterName
	 */
	public void deleteCounter(String counterName) {
		metricCommandTemplate.executeMetricDelete(counterName, MetricType.COUNTER);
	};

	public void verifyExists(String counterName) {
		metricCommandTemplate.checkIfMetricExists(DEFAULT_METRIC_NAME, MetricType.COUNTER);
	}

	public void deleteDefaultCounter() {
		if (metricCommandTemplate.isMetricAvailable(DEFAULT_METRIC_NAME, MetricType.COUNTER)) {
			metricCommandTemplate.executeMetricDelete(DEFAULT_METRIC_NAME, MetricType.COUNTER);
		}
	}

}
