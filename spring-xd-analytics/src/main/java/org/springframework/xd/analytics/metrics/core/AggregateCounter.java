package org.springframework.xd.analytics.metrics.core;

import java.util.Map;

/**
 * @author Mark Pollack
 * @author Luke Taylor
 */
public class AggregateCounter implements Metric {

	private final String id;

	private int totalCount;

	private final Map<Integer,Integer>  yearlyCounts;
	private final Map<Integer, Integer>  monthlyCountsForYear;
	private final Map<Integer, Integer> dayCountsForMonth;
	private final Map<Integer, Integer> hourCountsForDay;
	private final Map<Integer, Integer> minCountsForHour;

	public AggregateCounter(String id, int totalCount,
			Map<Integer, Integer> yearlyCounts,
			Map<Integer, Integer> monthlyCountsForYear,
			Map<Integer, Integer> dayCountsForMonth,
			Map<Integer, Integer> hourCountsForDay,
			Map<Integer, Integer> minCountsForHour) {
		this.id = id;
		this.totalCount = totalCount;
		this.yearlyCounts = yearlyCounts;
		this.monthlyCountsForYear = monthlyCountsForYear;
		this.dayCountsForMonth = dayCountsForMonth;
		this.hourCountsForDay = hourCountsForDay;
		this.minCountsForHour = minCountsForHour;
	}
	/**
	 * @return the id
	 */
	public String getName() {
		return id;
	}

	/**
	 * @return the totalCount
	 */
	public int getTotalCount() {
		return totalCount;
	}

	/**
	 * @return the yearlyCounts
	 */
	public Map<Integer, Integer> getYearlyCounts() {
		return yearlyCounts;
	}

	/**
	 * @return the monthlyCountsForYear
	 */
	public Map<Integer, Integer> getMonthlyCountsForYear() {
		return monthlyCountsForYear;
	}

	/**
	 * @return the dayCountsForMonth
	 */
	public Map<Integer, Integer> getDayCountsForMonth() {
		return dayCountsForMonth;
	}

	/**
	 * @return the hourCountsForDay
	 */
	public Map<Integer, Integer> getHourCountsForDay() {
		return hourCountsForDay;
	}
	/**
	 * @return the minCountsForHour
	 */
	public Map<Integer, Integer> getMinCountsForHour() {
		return minCountsForHour;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		AggregateCounter that = (AggregateCounter) o;

		if (!id.equals(that.id)) return false;

		return true;
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}
}

