/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.xd.shell.util;

import java.util.Arrays;

import org.springframework.util.Assert;

/**
 * Various utility methods when dealing with shell commands.
 * 
 * @author Eric Bottard
 */
public class Assertions {

	private Assertions() {

	}

	/**
	 * Accepts 2*N arguments, even ones being names and odd ones being values for those names. Asserts that exactly only
	 * one value is non null (or non-false, Boolean.FALSE being treated as false), or throws an exception with a
	 * descriptive message otherwise.
	 * 
	 * @return the index of the "pair" that was not {@code null}
	 * @throws IllegalArgumentException if more than one argument is non null
	 * @throws IllegalStateException if the method is called with wrong values (e.g. non even number of args)
	 */
	public static int exactlyOneOf(Object... namesAndValues) {
		int index = atMostOneOf(namesAndValues);
		Assert.isTrue(index >= 0, "You must specify exactly one of " + collectNames(namesAndValues));
		return index;
	}

	/**
	 * Accepts 2*N arguments, even ones being names and odd ones being values for those names. Asserts that at most one
	 * value is non null (or non-false, Boolean.FALSE being treated as false), or throws an exception with a descriptive
	 * message otherwise.
	 * 
	 * @return the index of the "pair" that was not {@code null}, or -1 if none was set
	 * @throws IllegalArgumentException if more than one argument is non null
	 * @throws IllegalStateException if the method is called with wrong values (e.g. non even number of args)
	 */
	public static int atMostOneOf(Object... namesAndValues) {
		int index = -1;
		Assert.state(namesAndValues.length % 2 == 0,
				"Expected even number of arguments: " + Arrays.asList(namesAndValues));
		for (int i = 0; i < namesAndValues.length / 2; i++) {
			Assert.state(namesAndValues[i * 2] instanceof String, "Argument at position " + i
					+ " should be argument name");
			if (namesAndValues[i * 2 + 1] != null && namesAndValues[i * 2 + 1] != Boolean.FALSE) {
				if (index != -1) {
					throw new IllegalArgumentException(String.format("You can't specify both '%s' and '%s'",
							namesAndValues[index * 2], namesAndValues[i * 2]));
				}
				else {
					index = i;
				}
			}
		}
		return index;
	}

	/**
	 * Returns the names (even values) out of the given array.
	 */
	private static String collectNames(Object[] namesAndValues) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < namesAndValues.length; i += 2) {
			sb.append('\'').append(namesAndValues[i]).append("', ");
		}
		return sb.substring(0, sb.length() - 2); // chop last ", "
	}
}
