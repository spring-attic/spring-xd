package org.springframework.xd.analytics.metrics.core;

import org.joda.time.*;

/**
 *
 * @author Luke Taylor
 */
public enum AggregateCountResolution {
	minute(Minutes.minutes(1)),
	hour(Hours.hours(1)),
	day(Days.days(1)),
	month(Months.months(1));

	public final ReadablePeriod unitPeriod;

	private AggregateCountResolution(ReadablePeriod unitPeriod) {
		this.unitPeriod = unitPeriod;
	}
}
