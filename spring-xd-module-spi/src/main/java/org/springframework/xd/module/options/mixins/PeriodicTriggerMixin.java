/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.xd.module.options.mixins;

import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;

import org.springframework.xd.module.options.spi.ModuleOption;


/**
 * Mixin for options that include a periodic fixed-delay trigger.
 * fixedDelay remains in annotated classes to avoid breaking changes
 * due to differing defaults.
 *
 * @author Gary Russell
 */
public class PeriodicTriggerMixin {

	private int initialDelay = 0;

	private String timeUnit = "SECONDS";

	@Min(0)
	public int getInitialDelay() {
		return initialDelay;
	}

	@ModuleOption("an initial delay when using a fixed delay trigger, expressed in TimeUnits (seconds by default)")
	public void setInitialDelay(int initialDelay) {
		this.initialDelay = initialDelay;
	}

	@Pattern(regexp = "(?i)(NANOSECONDS|MICROSECONDS|MILLISECONDS|SECONDS|MINUTES|HOURS|DAYS)",
			message = "timeUnit must be one of NANOSECONDS, MICROSECONDS, MILLISECONDS, SECONDS, MINUTES, HOURS, DAYS (case-insensitive)")
	public String getTimeUnit() {
		return timeUnit;
	}

	@ModuleOption("the time unit for the fixed and initial delays")
	public void setTimeUnit(String timeUnit) {
		this.timeUnit = timeUnit.toUpperCase();
	}

}
