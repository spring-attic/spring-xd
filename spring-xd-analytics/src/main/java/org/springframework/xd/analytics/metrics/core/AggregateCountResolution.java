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
package org.springframework.xd.analytics.metrics.core;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Hours;
import org.joda.time.Minutes;
import org.joda.time.Months;
import org.joda.time.ReadablePeriod;
import org.joda.time.Years;

/**
 * The resolution options available for querying an aggregate counter.
 *
 * @author Luke Taylor
 */
public enum AggregateCountResolution {
	minute(Minutes.minutes(1)),
	hour(Hours.hours(1)),
	day(Days.days(1)),
	month(Months.months(1)),
	year(Years.years(1));

	public final ReadablePeriod unitPeriod;

	private AggregateCountResolution(ReadablePeriod unitPeriod) {
		this.unitPeriod = unitPeriod;
	}

	/**
	 * Subtracts this resolution a given number of times from a supplied date.
	 *
	 * @param dt the date to subtract from
	 * @param n the number of periods of this resolution to subtract
	 * @return the resulting date in the past.
	 */
	public DateTime minus(DateTime dt, int n) {
		DateTime start = dt;
		for (int i = 0; i < n; i++) {
			start = start.minus(unitPeriod);
		}
		return start;
	}
}
