/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.xd.module.options.validation;

/**
 * Utility class for assisting in checking that mutually exclusive options are correctly set.
 *
 * @author Eric Bottard
 */
public class Exclusives {

	/**
	 * Return true when 0 or 1 boolean are true, false otherwise.
	 */
	public static boolean atMostOneOf(boolean... values) {
		return numberOfTrues(values) <= 1;
	}

	/**
	 * Return true when 2 or more booleans are true, false otherwise.
	 */
	public static boolean strictlyMoreThanOne(boolean... bs) {
		return numberOfTrues(bs) > 1;
	}

	public static int numberOfTrues(boolean... bs) {
		int result = 0;
		for (boolean b : bs) {
			if (b) {
				result++;
			}
		}
		return result;
	}
}
