/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.rest.domain.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

/**
 * Provides a commons set of time-related helper methods and also defines common
 * date/time formats.
 *
 * @author Gunnar Hillert
 * @since 1.0
 */
public final class TimeUtils {

	public static final String DEFAULT_XD_DATE_FORMAT_PATTERN = "yyyy-MM-dd";

	public static final String DEFAULT_XD_TIME_FORMAT_PATTERN = "HH:mm:ss";

	public static final String DEFAULT_XD_DATE_TIME_FORMAT_PATTERN = "yyyy-MM-dd HH:mm:ss,SSS";

	public static final String DEFAULT_XD_DURATION_FORMAT_PATTERN = "HH:mm:ss";

	public static final String DEFAULT_XD_TIMEZONE_ID = "UTC";

	/** Prevent instantiation. */
	private TimeUtils() {
		throw new AssertionError();
	}

	/**
	 * @return Default Spring XD {@link TimeZone} using ID {@link TimeUtils#DEFAULT_XD_TIMEZONE_ID}
	 */
	public static TimeZone getDefaultTimeZone() {
		return TimeZone.getTimeZone(DEFAULT_XD_TIMEZONE_ID);
	}

	/**
	 * @return The JVM {@link TimeZone}.
	 */
	public static TimeZone getJvmTimeZone() {
		return TimeZone.getDefault();
	}

	/**
	 * @return The Default Spring XD {@link DateFormat} using pattern {@link TimeUtils#DEFAULT_XD_DATE_FORMAT_PATTERN}
	 */
	public static DateFormat getDefaultDateFormat() {
		return new SimpleDateFormat(DEFAULT_XD_DATE_FORMAT_PATTERN);
	}

	/**
	 * @return The Default Spring XD time format using {@link DateFormat} pattern {@link TimeUtils#DEFAULT_XD_TIME_FORMAT_PATTERN}
	 */
	public static DateFormat getDefaultTimeFormat() {
		return new SimpleDateFormat(DEFAULT_XD_TIME_FORMAT_PATTERN);
	}

	/**
	 * @return The Default Spring XD date/time format using {@link DateFormat} pattern {@link TimeUtils#DEFAULT_XD_DATE_TIME_FORMAT_PATTERN}
	 */
	public static DateFormat getDefaultDateTimeFormat() {
		return new SimpleDateFormat(DEFAULT_XD_DATE_TIME_FORMAT_PATTERN);
	}

	/**
	 * @return The Default Spring XD duration format using {@link DateFormat} pattern {@link TimeUtils#DEFAULT_XD_DURATION_FORMAT_PATTERN}
	 */
	public static DateFormat getDefaultDurationFormat() {
		return new SimpleDateFormat(DEFAULT_XD_DURATION_FORMAT_PATTERN);
	}

}
