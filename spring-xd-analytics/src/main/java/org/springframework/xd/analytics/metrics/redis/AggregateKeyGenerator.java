package org.springframework.xd.analytics.metrics.redis;

import org.joda.time.DateTime;
import org.joda.time.ReadableDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.util.Assert;

/**
 * @author Mark Pollack
 * @author Luke Taylor
 */
class AggregateKeyGenerator {

	final static DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("yyyyMMddHHmm");

	public final static String SEPARATOR = ":";

	//keys
	private final String hourKey;
	private final String dayKey;
	private final String monthKey;
	private final String yearKey;
	private final String yearsKey;
	private final String totalKey;
	//time
	private final String year;
	private final String minute;
	private final String hour;
	private final String day;
	private final String month;

	private final String rootKey;
	private final String counterName;
	
	public AggregateKeyGenerator(String counterName) {
		this(counterName, new DateTime());
	}

	public AggregateKeyGenerator(String counterName, ReadableDateTime dateTime) {
		Assert.notNull(counterName, "Counter name name can not be null");
		Assert.notNull(dateTime, "DateTime can not be null");
		this.rootKey = "counters";
		this.counterName = counterName;
		String timeStamp = dateTimeFormatter.print(dateTime);
		totalKey = key("total");
		hourKey = key(timeStamp.substring(0,10));
		dayKey = key(timeStamp.substring(0,8));
		monthKey = key(timeStamp.substring(0,6));
		yearKey = key(timeStamp.substring(0,4));
		yearsKey = key("years");

		minute = timeStamp.substring(10,12);
		hour = timeStamp.substring(8,10);
		day = timeStamp.substring(6,8);
		month = timeStamp.substring(4,6);
		year = timeStamp.substring(0,4);
	}

	/**
	 * @return the yearsKey
	 */
	public String getYearsKey() {
		return yearsKey;
	}

	/**
	 * @return the totalKey
	 */
	public String getTotalKey() {
		return totalKey;
	}

	private String key(String suffix) {
		return rootKey + SEPARATOR + counterName + SEPARATOR + suffix;
	}
	
//	public String getFullCounterName() {
//		return rootKey + SEPARATOR + counterName;
//	}

	/**
	 * @return the hourKey
	 */
	public String getHourKey() {
		return hourKey;
	}

	/**
	 * @return the dayKey
	 */
	public String getDayKey() {
		return dayKey;
	}

	/**
	 * @return the monthKey
	 */
	public String getMonthKey() {
		return monthKey;
	}

	/**
	 * @return the yearKey
	 */
	public String getYearKey() {
		return yearKey;
	}

	/**
	 * @return the minute
	 */
	public String getMinute() {
		return minute;
	}

	/**
	 * @return the hour
	 */
	public String getHour() {
		return hour;
	}

	/**
	 * @return the day
	 */
	public String getDay() {
		return day;
	}

	/**
	 * @return the month
	 */
	public String getMonth() {
		return month;
	}

	public String getYear() {
		return this.year;
	}

}
