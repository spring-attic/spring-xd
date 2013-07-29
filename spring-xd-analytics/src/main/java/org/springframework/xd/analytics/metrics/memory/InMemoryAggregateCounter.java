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
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeField;
import org.joda.time.Duration;
import org.joda.time.DurationField;
import org.joda.time.Interval;
import org.springframework.xd.analytics.metrics.core.AggregateCount;
import org.springframework.xd.analytics.metrics.core.AggregateCounter;
import org.springframework.xd.analytics.metrics.core.MetricUtils;

/**
 * In-Memory implementation of {@link AggregateCounter} that adds logic for computing the
 * different aggregates. Extracted in a specific sub-class so as to not force a contract
 * on other implementations that may wish to leverage a datastore to keep track of the
 * aggregates.
 * 
 * @author Eric Bottard
 */
public class InMemoryAggregateCounter extends AggregateCounter {

	/**
	 * Copy constructor, may be used to convert from a plain {@link AggregateCounter} to
	 * an in memory-one.
	 */
	public InMemoryAggregateCounter(AggregateCounter c) {
		super(c);
	}

	/**
	 * Create a new named {@link AggregateCounter}.
	 */
	public InMemoryAggregateCounter(String name) {
		super(name);
	}

	public AggregateCount getCounts(Interval interval, DateTimeField resolution) {
		DurationField resolutionDuration = resolution.getDurationField();
		DateTime start = interval.getStart();
		DateTime end = interval.getEnd();

		int[] counts;
		if (resolutionDuration.getUnitMillis() == DateTimeConstants.MILLIS_PER_MINUTE) {
			DateTime now = start;
			List<int[]> days = accumulateDayCounts(minuteCountsByDay, start, end, 60 * 24);

			counts = MetricUtils.concatArrays(days, interval.getStart().getMinuteOfDay(),
					interval.toPeriod().toStandardMinutes().getMinutes(), 24 * 60);
		}
		else if (resolutionDuration.getUnitMillis() == DateTimeConstants.MILLIS_PER_HOUR) {
			DateTime now = start;
			List<int[]> days = accumulateDayCounts(hourCountsByDay, start, end, 24);

			counts = MetricUtils.concatArrays(days, interval.getStart().getHourOfDay(),
					interval.toPeriod().toStandardHours().getHours(), 24);
		}
		else {
			throw new IllegalArgumentException("Only minute or hour resolution is currently supported");
		}
		return new AggregateCount(name, interval, counts, resolution);
	}

	private static List<int[]> accumulateDayCounts(Map<Integer, int[]> fromDayCounts, DateTime start, DateTime end,
			int subSize) {
		List<int[]> days = new ArrayList<int[]>();
		Duration step = Duration.standardDays(1);
		int[] emptySubArray = new int[subSize];
		end = end.plusDays(1); // Need to account for an interval which crosses days

		for (DateTime now = start; now.isBefore(end); now = now.plus(step)) {
			int countsByDayKey = now.getYear() * 1000 + now.getDayOfYear();
			int[] dayCounts = fromDayCounts.get(countsByDayKey);

			if (dayCounts == null) {
				// Use an empty array if we don't have data
				dayCounts = emptySubArray;
			}
			days.add(dayCounts);
		}
		return days;
	}

	synchronized long increment(int amount, DateTime dateTime) {
		int year = dateTime.getYear();
		int month = dateTime.getMonthOfYear();
		int day = dateTime.getDayOfYear();
		int hour = dateTime.getHourOfDay();
		int minute = dateTime.getMinuteOfDay();

		int[] monthCounts = monthCountsByYear.get(year);
		int[] dayCounts = dayCountsByYear.get(year);

		if (monthCounts == null) {
			monthCounts = new int[12];
			monthCountsByYear.put(year, monthCounts);
			Duration d = new Duration(new DateTime(year, 1, 1, 0, 0, 0), new DateTime(year + 1, 1, 1, 0, 0, 0));
			dayCounts = new int[(int) d.toDuration().getStandardDays()];
			dayCountsByYear.put(year, dayCounts);
		}

		int countsByDayKey = year * 1000 + day;
		int[] hourCounts = hourCountsByDay.get(countsByDayKey);

		if (hourCounts == null) {
			hourCounts = new int[24];
			hourCountsByDay.put(countsByDayKey, hourCounts);
		}

		int[] minuteCounts = minuteCountsByDay.get(countsByDayKey);

		if (minuteCounts == null) {
			minuteCounts = new int[60 * 24];
			minuteCountsByDay.put(countsByDayKey, minuteCounts);
		}

		minuteCounts[minute] += amount;
		monthCounts[month] += amount;
		dayCounts[day] += amount;
		hourCounts[hour] += amount;
		total += amount;
		return total;
	}

}
