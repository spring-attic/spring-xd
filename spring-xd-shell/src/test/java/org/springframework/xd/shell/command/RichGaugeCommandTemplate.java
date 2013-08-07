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
 * @author Ilayaperumal Gopinathan
 */
public class RichGaugeCommandTemplate extends AbstractCommandTemplate {

	private MetricCommandTemplate metricCommandTemplate;

	/**
	 * Construct a new CounterCommandTemplate, given a spring shell.
	 * 
	 * @param shell the spring shell to execute commands against
	 */
	public RichGaugeCommandTemplate(JLineShellComponent shell) {
		super(shell);
		metricCommandTemplate = new MetricCommandTemplate(shell);
	}

	public void verifyRichGauge(String expectedValue) {
		verifyRichGauge(DEFAULT_METRIC_NAME, expectedValue);
	}

	public void verifyRichGauge(String counterName, String expectedValue) {
		metricCommandTemplate.checkIfMetricExists(counterName, MetricType.RICH_GAUGE);
		metricCommandTemplate.checkMetricValue(counterName, MetricType.RICH_GAUGE, expectedValue);
	}

	/**
	 * Deletes the given counter
	 * 
	 * @param counterName
	 */
	public void deleteRichGauge(String counterName) {
		metricCommandTemplate.executeMetricDelete(counterName, MetricType.RICH_GAUGE);
	};

	public void deleteDefaultRichGauge() {
		if (metricCommandTemplate.isMetricAvailable(DEFAULT_METRIC_NAME, MetricType.RICH_GAUGE)) {
			metricCommandTemplate.executeMetricDelete(DEFAULT_METRIC_NAME, MetricType.RICH_GAUGE);
		}
	}

	public void verifyDefaultExists() {
		metricCommandTemplate.checkIfMetricExists(DEFAULT_METRIC_NAME, MetricType.RICH_GAUGE);
	}

}
