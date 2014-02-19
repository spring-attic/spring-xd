/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.xd.shell.util;

import org.hamcrest.Description;
import org.hamcrest.DiagnosingMatcher;
import org.hamcrest.Matcher;


/**
 * Contains hamcrest matchers against Tables.
 * 
 * @author Eric Bottard
 */
public class TableMatchers {

	/**
	 * A matcher that matches if contents of some column matches.
	 */
	public static Matcher<TableRow> rowWithColumn(final int columnNumber, final Matcher<String> matcher) {
		return new DiagnosingMatcher<TableRow>() {

			@Override
			public void describeTo(Description description) {
				description.appendText("a row with column ").appendValue(columnNumber).appendText(" ").appendDescriptionOf(
						matcher);
			}

			@Override
			protected boolean matches(Object item, Description mismatchDescription) {
				matcher.describeMismatch(item, mismatchDescription);
				TableRow row = (TableRow) item;
				return matcher.matches(row.getValue(columnNumber));
			}
		};
	}

}
