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

import java.util.Arrays;

import org.joda.time.Interval;

/**
 * Represents the data returned from an aggregate count query.
 *
 * @author Luke Taylor
 */
public class AggregateCount {

	private final String name;

	private final Interval interval;

	private final long[] counts;

	private final AggregateCountResolution resolution;

	public AggregateCount(String name, Interval interval, long[] counts, AggregateCountResolution resolution) {
		this.name = name;
		this.interval = interval;
		this.counts = counts.clone();
		this.resolution = resolution;
	}

	/**
	 * @return the total number of counts in the interval.
	 */
	public int getTotal() {
		int total = 0;
		for (int i = 0; i < counts.length; i++) {
			total += counts[i];
		}
		return total;
	}

	public String getName() {
		return name;
	}

	public Interval getInterval() {
		return interval;
	}

	public long[] getCounts() {
		return counts;
	}

	public AggregateCountResolution getResolution() {
		return resolution;
	}

	@Override
	public String toString() {
		return "AggregateCount{" +
				"name='" + name + '\'' +
				", interval=" + interval +
				", counts=" + Arrays.toString(counts) +
				", resolution=" + resolution +
				'}';
	}
}

