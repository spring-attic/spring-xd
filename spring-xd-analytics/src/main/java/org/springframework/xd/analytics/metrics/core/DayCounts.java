/*
 * Copyright 2011-2013 the original author or authors.
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

package org.springframework.xd.analytics.metrics.core;

import java.util.concurrent.TimeUnit;

import org.joda.time.DateMidnight;

/**
 * Represents the report of an aggregate counter obtained for a single (possibly partial) day.
 *
 * The "resolution" property defines the granularity of the counts available. Currently supports
 * minute-level resolution.
 *
 * @author Luke Taylor
 */
public class DayCounts {
	private TimeUnit resolution = TimeUnit.MINUTES;
	private final String name;
	private final int[] hourCounts;
	private final int[] minuteCounts;
	private final DateMidnight date;

	public DayCounts(String name, DateMidnight date, int[] hourCounts) {
		this(name, hourCounts, date, new int[0]);
	}

	public DayCounts(String name, int[] hourCounts, DateMidnight date, int[] minuteCounts) {
		if (hourCounts == null || minuteCounts == null) {
			throw new IllegalArgumentException("Count arrays cannot be null");
		}
		if (hourCounts.length == 0 && minuteCounts.length == 0) {
			throw new IllegalArgumentException("Count arrays must have some content");
		}
		if (hourCounts.length * 60 < minuteCounts.length) {
			throw new IllegalArgumentException("Minute counts exceed number of hours");
		}
		if (minuteCounts.length == 0) {
			resolution = TimeUnit.HOURS;
		}
		this.name = name;
		this.date = date;
		this.hourCounts = hourCounts;
		this.minuteCounts = minuteCounts;
	}

	public boolean hasResolution(TimeUnit resolution) {
		return this.resolution.compareTo(resolution) <= 0;
	}

	public DateMidnight getDate() {
		return date;
	}

	public int[] getHourCounts() {
		return hourCounts;
	}

	public int[] getMinuteCounts() {
		if (resolution != TimeUnit.MINUTES) {
			throw new IllegalStateException("Minute resolution not available");
		}
		return minuteCounts;
	}
}

