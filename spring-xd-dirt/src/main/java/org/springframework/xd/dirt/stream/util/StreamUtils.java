/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.xd.dirt.stream.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.Assert;


/**
 * @author Gunnar Hillert
 * @since 1.0
 */
public class StreamUtils {

	/** Prevent instantiation. */
	private StreamUtils() {
		throw new AssertionError();
	}

	public static Pattern passwordParameterPattern = Pattern.compile(
			"(?i)(--[\\s|\\w]*(password|passwd)\\w*\\s*=[\\s]*)([\\w|.]*)",
			Pattern.UNICODE_CHARACTER_CLASS);

	/**
	 * This method takes a Stream definition String as input parameter. The method
	 * will filter out any parameters labeled {@code --password} and will masked them
	 * using the {@code *} character.
	 *
	 * @param streamDefinition Must not be empty
	 * @return The stream definition string with masked passwords. Should never return null.
	 */
	public static String maskPasswordsInStreamDefinition(String streamDefinition) {

		Assert.hasText(streamDefinition, "streamDefinition must be neither empty nor null.");

		final StringBuffer output = new StringBuffer();
		final Matcher matcher = passwordParameterPattern.matcher(streamDefinition);
		while (matcher.find()) {
			matcher.appendReplacement(output, matcher.group(1) + matcher.group(3).replaceAll(".", "*"));
		}
		matcher.appendTail(output);
		return output.toString();
	}
}
