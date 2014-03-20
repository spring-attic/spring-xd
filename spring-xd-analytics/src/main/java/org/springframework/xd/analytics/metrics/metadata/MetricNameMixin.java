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

import org.springframework.xd.module.options.spi.ModuleOption;


/**
 * Adds a {@code name} option, intended to capture the name of the metric.
 * 
 * @author Eric Bottard
 */
public class MetricNameMixin {

	// Default to null until ${xd.stream.name} is supported
	private String name = null;

	// @Pattern("[a-zA-Z0-9]+")
	public String getName() {
		return name;
	}

	@ModuleOption("the name of the metric to contribute to (will be created if necessary)")
	public void setName(String name) {
		this.name = name;
	}


}
