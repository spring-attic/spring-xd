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

package org.springframework.xd.analytics.metrics.memory;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.Chronology;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Duration;
import org.joda.time.Interval;

import org.joda.time.Months;
import org.joda.time.Years;
import org.springframework.util.Assert;
import org.springframework.xd.analytics.metrics.core.AggregateCount;
import org.springframework.xd.analytics.metrics.core.AggregateCountResolution;
import org.springframework.xd.analytics.metrics.core.Counter;
import org.springframework.xd.analytics.metrics.core.MetricUtils;

/**
 * A counter that tracks integral values but also remembers how its value was distributed over time.
 * 
 * <p>
 * This core class only holds data structures. Depending on backing stores, logic for computing totals may be
 * implemented in a specialization of this class or at the repository level.
 * </p>
 * 
 * @author Luke Taylor
 * @author Eric Bottard
 */
class InMemoryAggregateCounter extends Counter {
	private Map<Integer, long[]> monthCountsByYear = new HashMap<Integer, long[]>();

	private Map<Integer, long[]> dayCountsByYear = new HashMap<Integer, long[]>();

	private Map<Integer, long[]> hourCountsByDay = new HashMap<Integer, long[]>();

	private Map<Integer, long[]> minuteCountsByDay = new HashMap<Integer, long[]>();

	public InMemoryAggregateCounter(String name, long value) {
		super(name, value);
	}

	public InMemoryAggregateCounter(String name) {
		super(name);
	}

	public AggregateCount getCounts(int nCounts, DateTime endDate, AggregateCountResolution resolution) {
		Assert.notNull(endDate, "endDate must not be null");

		return getCounts(new Interval(resolution.minus(endDate, nCounts-1), endDate), resolution);
	}

	public AggregateCount getCounts(Interval interval, AggregateCountResolution resolution) {
		DateTime start = interval.getStart();
		DateTime end = interval.getEnd();
		Chronology c = interval.getChronology();

		long[] counts;
		if (resolution == AggregateCountResolution.minute) {
			List<long[]> days = accumulateDayCounts(minuteCountsByDay, start, end, 60 * 24);

			counts = MetricUtils.concatArrays(days, interval.getStart().getMinuteOfDay(),
					interval.toPeriod().toStandardMinutes().getMinutes() + 1);
		}
		else if (resolution == AggregateCountResolution.hour) {
			List<long[]> days = accumulateDayCounts(hourCountsByDay, start, end, 24);

			counts = MetricUtils.concatArrays(days, interval.getStart().getHourOfDay(),
					interval.toPeriod().toStandardHours().getHours() + 1);
		}
		else if (resolution == AggregateCountResolution.day) {
			DateTime startDay = new DateTime(c.dayOfYear().roundFloor(start.getMillis()));
			DateTime endDay = new DateTime(c.dayOfYear().roundFloor(end.plusDays(1).getMillis()));
			int nDays = Days.daysBetween(startDay, endDay).getDays();
			DateTime cursor = new DateTime(c.year().roundFloor(interval.getStartMillis()));
			List<long[]> yearDays = new ArrayList<long[]>();
			DateTime endYear = new DateTime(c.year().roundCeiling(end.getMillis()));

			while (cursor.isBefore(endYear)) {
				long[] dayCounts = dayCountsByYear.get(cursor.getYear());
				if (dayCounts == null) {
					// Querying where we have no data
					dayCounts = new long[daysInYear(cursor.getYear())];
				}
				yearDays.add(dayCounts);
				cursor = cursor.plusYears(1);
			}

			counts = MetricUtils.concatArrays(yearDays, startDay.getDayOfYear() - 1, nDays);

		}
		else if (resolution == AggregateCountResolution.month) {
			DateTime startMonth = new DateTime(c.monthOfYear().roundFloor(interval.getStartMillis()));
			DateTime endMonth = new DateTime(c.monthOfYear().roundFloor(end.plusMonths(1).getMillis()));
			int nMonths = Months.monthsBetween(startMonth, endMonth).getMonths();
			DateTime cursor = new DateTime(c.year().roundFloor(interval.getStartMillis()));
			List<long[]> yearMonths = new ArrayList<long[]>();
			DateTime endYear = new DateTime(c.year().roundCeiling(end.getMillis()));

			while (cursor.isBefore(endYear)) {
				long[] monthCounts = monthCountsByYear.get(cursor.getYear());
				if (monthCounts == null) {
					monthCounts = new long[12];
				}
				yearMonths.add(monthCounts);
				cursor = cursor.plusYears(1);
			}

			counts = MetricUtils.concatArrays(yearMonths, startMonth.getMonthOfYear() - 1 , nMonths);
		}
		else if (resolution == AggregateCountResolution.year) {
			DateTime startYear = new DateTime(interval.getStart().getYear(), 1, 1, 0, 0);
			DateTime endYear   =  new DateTime(end.getYear() + 1, 1, 1, 0, 0);
			int nYears = Years.yearsBetween(startYear, endYear).getYears();
			counts = new long[nYears];

			for (int i = 0; i < nYears; i++) {
				long[] monthCounts = monthCountsByYear.get(startYear.plusYears(i).getYear());
				counts[i] = MetricUtils.sum(monthCounts);
			}

		}
		else {
			throw new IllegalStateException("Shouldn't happen. Unhandled resolution: " + resolution);
		}
		return new AggregateCount(getName(), interval, counts, resolution);
	}

	private static List<long[]> accumulateDayCounts(Map<Integer, long[]> fromDayCounts, DateTime start, DateTime end,
			int subSize) {
		List<long[]> days = new ArrayList<long[]>();
		Duration step = Duration.standardDays(1);
		long[] emptySubArray = new long[subSize];
		end = end.plusDays(1); // Need to account for an interval which crosses days

		for (DateTime now = start; now.isBefore(end); now = now.plus(step)) {
			int countsByDayKey = now.getYear() * 1000 + now.getDayOfYear();
			long[] dayCounts = fromDayCounts.get(countsByDayKey);

			if (dayCounts == null) {
				// Use an empty array if we don't have data
				dayCounts = emptySubArray;
			}
			days.add(dayCounts);
		}
		return days;
	}

	private int daysInYear(int year) {
		Duration d = new Duration(new DateTime(year, 1, 1, 0, 0), new DateTime(year + 1, 1, 1, 0, 0));
		return (int)d.getStandardDays();
	}

	synchronized long increment(long amount, DateTime dateTime) {
		int year = dateTime.getYear();
		int month = dateTime.getMonthOfYear();
		int day = dateTime.getDayOfYear();
		int hour = dateTime.getHourOfDay();
		int minute = dateTime.getMinuteOfDay();

		long[] monthCounts = monthCountsByYear.get(year);
		long[] dayCounts = dayCountsByYear.get(year);

		if (monthCounts == null) {
			monthCounts = new long[12];
			monthCountsByYear.put(year, monthCounts);
			dayCounts = new long[daysInYear(year)];
			dayCountsByYear.put(year, dayCounts);
		}

		int countsByDayKey = year * 1000 + day;
		long[] hourCounts = hourCountsByDay.get(countsByDayKey);

		if (hourCounts == null) {
			hourCounts = new long[24];
			hourCountsByDay.put(countsByDayKey, hourCounts);
		}

		long[] minuteCounts = minuteCountsByDay.get(countsByDayKey);

		if (minuteCounts == null) {
			minuteCounts = new long[60 * 24];
			minuteCountsByDay.put(countsByDayKey, minuteCounts);
		}

		minuteCounts[minute] += amount;
		monthCounts[month-1] += amount;
		dayCounts[day-1] += amount;
		hourCounts[hour] += amount;

		return increment(amount);
	}

}
