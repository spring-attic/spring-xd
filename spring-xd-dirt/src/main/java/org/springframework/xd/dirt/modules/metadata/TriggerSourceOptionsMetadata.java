/*
 * Copyright 2013-2015 the original author or authors.
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

package org.springframework.xd.dirt.modules.metadata;

import javax.validation.constraints.AssertFalse;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;

import org.hibernate.validator.constraints.NotBlank;

import org.springframework.xd.module.options.mixins.MaxMessagesDefaultOneMixin;
import org.springframework.xd.module.options.mixins.PeriodicTriggerMixin;
import org.springframework.xd.module.options.spi.Mixin;
import org.springframework.xd.module.options.spi.ModuleOption;
import org.springframework.xd.module.options.spi.ProfileNamesProvider;
import org.springframework.xd.module.options.spi.ValidationGroupsProvider;
import org.springframework.xd.module.options.validation.CronExpression;
import org.springframework.xd.module.options.validation.DateFormat;
import org.springframework.xd.module.options.validation.DateWithCustomFormat;
import org.springframework.xd.module.options.validation.Exclusives;

/**
 * Describes options to the {@code trigger} source module.
 *
 * @author Eric Bottard
 * @author Florent Biville
 * @author Gary Russell
 */
@Mixin({ PeriodicTriggerMixin.class, MaxMessagesDefaultOneMixin.class })
@DateWithCustomFormat(groups = TriggerSourceOptionsMetadata.DateHasBeenSet.class)
public class TriggerSourceOptionsMetadata implements ProfileNamesProvider, ValidationGroupsProvider {

	private static final String[] USE_CRON = new String[] { "use-cron" };

	private static final String[] USE_DELAY = new String[] { "use-delay" };

	private static final String[] USE_DATE = new String[] { "use-date" };

	public static final String DEFAULT_DATE = "The current time";

	public static interface DateHasBeenSet {
	}

	private Integer fixedDelay;

	private String cron;

	private String payload = "";

	private String date = DEFAULT_DATE;

	private String dateFormat = "MM/dd/yy HH:mm:ss";

	@Override
	public String[] profilesToActivate() {
		if (cron != null) {
			return USE_CRON;
		}
		else if (fixedDelay != null) {
			return USE_DELAY;
		}
		else {
			return USE_DATE;
		}
	}

	@Min(0)
	public Integer getFixedDelay() {
		return fixedDelay;
	}

	@AssertFalse(message = "'cron', explicit 'date' and 'fixedDelay' are mutually exclusive")
	private boolean isInvalid() {
		return Exclusives.strictlyMoreThanOne(fixedDelay != null, cron != null, !DEFAULT_DATE.equals(date));
	}

	@ModuleOption("time delay between executions, expressed in TimeUnits (seconds by default)")
	public void setFixedDelay(Integer fixedDelay) {
		this.fixedDelay = fixedDelay;
	}


	@CronExpression
	public String getCron() {
		return cron;
	}

	@ModuleOption("cron expression specifying when the trigger should fire")
	public void setCron(String cron) {
		this.cron = cron;
	}


	public String getPayload() {
		return payload;
	}

	@ModuleOption("the message that will be sent when the trigger fires")
	public void setPayload(String payload) {
		this.payload = payload;
	}

	@NotNull
	public String getDate() {
		return date;
	}

	@ModuleOption("a one-time date when the trigger should fire; only applies if 'fixedDelay' and 'cron' are not provided")
	public void setDate(String date) {
		this.date = date;
	}

	@NotBlank
	@DateFormat
	public String getDateFormat() {
		return dateFormat;
	}

	@ModuleOption("the format specifying how the 'date' should be parsed")
	public void setDateFormat(String dateFormat) {
		this.dateFormat = dateFormat;
	}

	@Override
	public Class<?>[] groupsToValidate() {
		return DEFAULT_DATE.equals(date) ? DEFAULT_GROUP : new Class<?>[] { Default.class, DateHasBeenSet.class };
	}
}
