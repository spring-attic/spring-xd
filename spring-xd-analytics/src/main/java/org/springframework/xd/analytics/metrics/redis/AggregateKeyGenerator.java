/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.xd.analytics.metrics.redis;

import org.joda.time.DateTime;
import org.joda.time.ReadableDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import org.springframework.util.Assert;

/**
 * Utility class used to generate keys for a named Aggregate Counter.
 * <ol>
 * The general format is </li>
 * <li>One total value</li>
 * <li>One years hash with a field per year eg. { 2010: value, 2011: value }</li>
 * <li>One hash per year with a field per month { 01: value, ...}</li>
 * <li>One hash per month with a field per day</li>
 * <li>One hash per day with a field per hour</li>
 * <li>One hash per hour with a field per minute</li>
 * </ol>
 *
 * @author Mark Pollack
 * @author Luke Taylor
 */
/* default */class AggregateKeyGenerator {

	final static DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("yyyyMMddHHmm");

	public final static String SEPARATOR = ".";

	// keys
	private final String hourKey;

	private final String dayKey;

	private final String monthKey;

	private final String yearKey;

	private final String yearsKey;

	private final String totalKey;

	// time
	private final String year;

	private final String minute;

	private final String hour;

	private final String day;

	private final String month;

	private final String repoPrefix;

	private final String counterName;

	public AggregateKeyGenerator(String repoPrefix, String counterName) {
		this(repoPrefix, counterName, new DateTime());
	}

	public AggregateKeyGenerator(String repoPrefix, String counterName, ReadableDateTime dateTime) {
		Assert.notNull(counterName, "Counter name name can not be null");
		Assert.notNull(dateTime, "DateTime can not be null");
		this.repoPrefix = repoPrefix;
		this.counterName = counterName;
		String timeStamp = dateTimeFormatter.print(dateTime);
		totalKey = key("total");
		hourKey = key(timeStamp.substring(0, 10));
		dayKey = key(timeStamp.substring(0, 8));
		monthKey = key(timeStamp.substring(0, 6));
		yearKey = key(timeStamp.substring(0, 4));
		yearsKey = key("years");

		minute = timeStamp.substring(10, 12);
		hour = timeStamp.substring(8, 10);
		day = timeStamp.substring(6, 8);
		month = timeStamp.substring(4, 6);
		year = timeStamp.substring(0, 4);
	}

	public String getYearsKey() {
		return yearsKey;
	}

	public String getTotalKey() {
		return totalKey;
	}

	private String key(String suffix) {
		return repoPrefix + SEPARATOR + counterName + SEPARATOR + suffix;
	}

	public String getHourKey() {
		return hourKey;
	}

	public String getDayKey() {
		return dayKey;
	}

	public String getMonthKey() {
		return monthKey;
	}

	public String getYearKey() {
		return yearKey;
	}

	public String getMinute() {
		return minute;
	}

	public String getHour() {
		return hour;
	}

	public String getDay() {
		return day;
	}

	public String getMonth() {
		return month;
	}

	public String getYear() {
		return this.year;
	}

}
