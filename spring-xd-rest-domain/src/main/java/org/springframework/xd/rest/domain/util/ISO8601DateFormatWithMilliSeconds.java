/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.rest.domain.util;

import java.text.FieldPosition;
import java.util.Date;

import com.fasterxml.jackson.databind.util.ISO8601DateFormat;
import com.fasterxml.jackson.databind.util.ISO8601Utils;


/**
 * Improved version of the {@link ISO8601DateFormat} that also serializes milliseconds.
 *
 * @author Gunnar Hillert
 */
@SuppressWarnings("serial")
public class ISO8601DateFormatWithMilliSeconds extends ISO8601DateFormat {

	@Override
	public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition fieldPosition)
	{
		final String value = ISO8601Utils.format(date, true);
		toAppendTo.append(value);
		return toAppendTo;
	}

}
