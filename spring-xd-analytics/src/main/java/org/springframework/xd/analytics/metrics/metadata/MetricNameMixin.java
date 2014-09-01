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

package org.springframework.xd.analytics.metrics.metadata;

import static org.springframework.xd.module.options.spi.ModulePlaceholders.XD_STREAM_NAME;

import javax.validation.constraints.AssertFalse;

import org.springframework.xd.module.options.spi.ModuleOption;


/**
 * Adds a {@code name} option, intended to capture the name of the metric.
 *
 * @author Eric Bottard
 */
public class MetricNameMixin {

	private String name = XD_STREAM_NAME;

	private String nameExpression = null;

	@ModuleOption(value = "the name of the metric to contribute to (will be created if necessary)", defaultValue = XD_STREAM_NAME)
	public void setName(String name) {
		this.name = name;
	}

	@ModuleOption("a SpEL expression to compute the name of the metric to contribute to")
	public void setNameExpression(String nameExpression) {
		this.nameExpression = nameExpression;
	}

	@AssertFalse(message = "only one of 'name' or 'nameExpression' is allowed")
	private boolean isInvalidName() {
		return !valuesAreSetToDefaults()
				&& !(nameExpression != null ^ !XD_STREAM_NAME.equals(name));
	}

	private boolean valuesAreSetToDefaults() {
		return nameExpression == null && XD_STREAM_NAME.equals(name);
	}

	public String getComputedNameExpression() {
		return nameExpression != null ? nameExpression : ("'" + name + "'");
	}

}
