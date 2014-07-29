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

package org.springframework.xd.dirt.rest;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.Assert;
import org.springframework.xd.dirt.core.BaseDefinition;
import org.springframework.xd.dirt.stream.JobDefinition;
import org.springframework.xd.dirt.stream.StreamDefinition;


/**
 * Provides common utility methods for {@link JobDefinition}s and
 * {@link StreamDefinition}s ({@link BaseDefinition}), e.g. the masking
 * of passwords in definition parameters.
 *
 * @author Gunnar Hillert
 * @since 1.0
 */
public class PasswordUtils {

	/** Prevent instantiation. */
	private PasswordUtils() {
		throw new AssertionError();
	}

	public static Pattern passwordParameterPattern = Pattern.compile(
			"(?i)(--[\\p{Z}]*(password|passwd)[\\p{Z}]*=[\\p{Z}]*)([\\p{N}|\\p{L}|\\p{Po}]*)",
			Pattern.UNICODE_CASE);

	/**
	 * This method takes a definition String as input parameter. The method
	 * will filter out any parameters labeled {@code --password} and will masked them
	 * using the {@code *} character.
	 *
	 * @param definition Must not be empty
	 * @return The definition string with masked passwords. Should never return null.
	 */
	public static String maskPasswordsInDefinition(String definition) {

		Assert.hasText(definition, "definition must be neither empty nor null.");

		final StringBuffer output = new StringBuffer();
		final Matcher matcher = passwordParameterPattern.matcher(definition);
		while (matcher.find()) {
			matcher.appendReplacement(output, matcher.group(1) + matcher.group(3).replaceAll(".", "*"));
		}
		matcher.appendTail(output);
		return output.toString();
	}
}
