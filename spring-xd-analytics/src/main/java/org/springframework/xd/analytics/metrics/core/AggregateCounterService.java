package org.springframework.xd.analytics.metrics.core;

import org.joda.time.DateTime;
import org.joda.time.DateTimeField;
import org.joda.time.Interval;

/**
 * @author Luke Taylor
 */
public interface AggregateCounterService {
	void increment(String name);

	void increment(String name, int amount, DateTime dateTime);

	int getTotalCounts(String name);

	AggregateCount getCounts(String name, Interval interval, DateTimeField resolution);
}

