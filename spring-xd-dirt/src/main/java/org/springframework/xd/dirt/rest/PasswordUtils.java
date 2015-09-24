/*
 * Copyright 2014-2015 the original author or authors.
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

import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
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

	public static final String[] PASSWORD_PARAMETER_NAMES = { "password", "passwd" };

	public static final String MASK_CHARACTER = "*";

	public static Pattern passwordParameterPattern = Pattern.compile(
			//"(?i)(--[\\p{Z}]*(password|passwd)[\\p{Z}]*=[\\p{Z}]*)([\\p{N}|\\p{L}|\\p{Po}]*)",
			"(?i)(--[\\p{Z}]*[\\p{L}]*("
					+ StringUtils.arrayToDelimitedString(PASSWORD_PARAMETER_NAMES, "|")
					+ ")[\\p{L}]*[\\p{Z}]*=[\\p{Z}]*)((\"[\\p{L}|\\p{Pd}|\\p{Ps}|\\p{Pe}|\\p{Pc}|\\p{S}|\\p{N}|\\p{Z}]*\")|([\\p{N}|\\p{L}|\\p{Po}|\\p{Pc}|\\p{S}]*))",
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
			final String passwordValue = matcher.group(3);
			final String maskedPasswordValue;
			if (passwordValue.startsWith("\"") && passwordValue.endsWith("\"")) {
				final String passwordValueWithoutQuotes = passwordValue.substring(1, passwordValue.length() - 1);
				maskedPasswordValue = "\"" + maskString(passwordValueWithoutQuotes) + "\"";
			}
			else {
				maskedPasswordValue = maskString(matcher.group(3));
			}
			matcher.appendReplacement(output, matcher.group(1) + maskedPasswordValue);
		}
		matcher.appendTail(output);
		return output.toString();
	}

	/**
	 * Masks provided {@link String}s with the {@link PasswordUtils#MASK_CHARACTER}.
	 *
	 * @param stringToMask Must not be null
	 * @return The masked String
	 */
	public static String maskString(String stringToMask) {
		Assert.notNull(stringToMask, "'stringToMask' must not be null.");
		return stringToMask.replaceAll(".", MASK_CHARACTER);
	}

	/**
	 * Will mask property values if they contain keys that contain the values specified
	 * in {@link PasswordUtils#PASSWORD_PARAMETER_NAMES}.
	 *
	 * Be aware that this is a mutable operation and the passed-in {@link Properties}
	 * will be modified.
	 *
	 * @param properties Must not be null.
	 */
	public static void maskPropertiesIfNecessary(Properties properties) {
		Assert.notNull(properties, "'properties' must not be null.");

		for (Entry<Object, Object> property : properties.entrySet()) {
			if (property.getKey() instanceof String && property.getValue() instanceof String) {
				final String propertyKey = (String) property.getKey();
				final String propertyValue = (String) property.getValue();

				for (String param : PasswordUtils.PASSWORD_PARAMETER_NAMES) {
					if (propertyKey.toLowerCase().contains(param.toLowerCase())) {
						properties.setProperty(propertyKey,
								PasswordUtils.maskString(propertyValue));
					}
				}
			}
		}
	}
}
