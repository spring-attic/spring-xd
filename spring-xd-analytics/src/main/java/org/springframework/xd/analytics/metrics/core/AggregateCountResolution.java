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
