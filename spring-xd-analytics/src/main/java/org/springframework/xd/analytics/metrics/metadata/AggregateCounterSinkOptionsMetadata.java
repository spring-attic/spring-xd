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

import org.springframework.xd.module.options.mixins.DateFormatMixin;
import org.springframework.xd.module.options.spi.Mixin;
import org.springframework.xd.module.options.spi.ModuleOption;


/**
 * Captures options for the {@code aggregate-counter} sink module.
 *
 * @author Eric Bottard
 */
@Mixin({ DateFormatMixin.class, MetricNameMixin.class })
public class AggregateCounterSinkOptionsMetadata {

	private String timeField = "null";

	private String incrementExpression = "1";

	public String getTimeField() {
		return timeField;
	}

	@ModuleOption("name of a field in the message that contains the timestamp to contribute to")
	public void setTimeField(String timeField) {
		this.timeField = timeField;
	}

	public String getIncrementExpression() {
		return incrementExpression;
	}

	@ModuleOption("how much to increment each bucket, as a SpEL against the message")
	public void setIncrementExpression(String incrementExpression) {
		this.incrementExpression = incrementExpression;
	}

}
