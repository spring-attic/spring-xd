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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeField;
import org.joda.time.Duration;
import org.joda.time.DurationField;
import org.joda.time.Interval;

import org.springframework.xd.analytics.metrics.core.AggregateCount;
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

	public AggregateCount getCounts(Interval interval, DateTimeField resolution) {
		DurationField resolutionDuration = resolution.getDurationField();
		DateTime start = interval.getStart();
		DateTime end = interval.getEnd();

		long[] counts;
		if (resolutionDuration.getUnitMillis() == DateTimeConstants.MILLIS_PER_MINUTE) {
			List<long[]> days = accumulateDayCounts(minuteCountsByDay, start, end, 60 * 24);

			counts = MetricUtils.concatArrays(days, interval.getStart().getMinuteOfDay(),
					interval.toPeriod().toStandardMinutes().getMinutes() + 1);
		}
		else if (resolutionDuration.getUnitMillis() == DateTimeConstants.MILLIS_PER_HOUR) {
			List<long[]> days = accumulateDayCounts(hourCountsByDay, start, end, 24);

			counts = MetricUtils.concatArrays(days, interval.getStart().getHourOfDay(),
					interval.toPeriod().toStandardHours().getHours() + 1);
		}
		else if (resolutionDuration.getUnitMillis() == DateTimeConstants.MILLIS_PER_DAY) {
			DateTime startDay = new DateTime(interval.getChronology().dayOfYear().roundFloor(start.getMillis()));
			DateTime endDay = new DateTime(interval.getChronology().dayOfYear().roundFloor(end.plusDays(1).getMillis()));
			Interval rounded = new Interval(startDay, endDay);
			int nDays = rounded.toDuration().toStandardDays().getDays();
			List<long[]> yearDays = new ArrayList<long[]>();

			for (DateTime now = startDay; now.isBefore(endDay); now = now.plusYears(1)) {
				yearDays.add(dayCountsByYear.get(now.getYear()));
			}

			counts = MetricUtils.concatArrays(yearDays, startDay.getDayOfYear(), nDays);

		}
		else {
			throw new IllegalArgumentException("Only minute, hour or day resolution is currently supported");
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
			Duration d = new Duration(new DateTime(year, 1, 1, 0, 0, 0), new DateTime(year + 1, 1, 1, 0, 0, 0));
			dayCounts = new long[(int) d.toDuration().getStandardDays()];
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
		monthCounts[month] += amount;
		dayCounts[day] += amount;
		hourCounts[hour] += amount;

		return increment(amount);
	}

}
