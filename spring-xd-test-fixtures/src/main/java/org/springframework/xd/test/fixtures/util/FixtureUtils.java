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

package org.springframework.xd.test.fixtures.util;

import org.springframework.util.StringUtils;


/**
 * Utility to support common tasks done by fixtures.
 * @author Glenn Renfro
 */
public class FixtureUtils {

	/**
	 * Returns a properly formatted label for a module in a stream definition.
	 * @param label The label to be formatted.
	 * @return If label is not empty nor null a properly formatted label for module else an empty string.
	 */
	public static String labelOrEmpty(String label) {
		return (StringUtils.hasLength(label)) ? label + ":" : "";
	}

	/**
	 * Fixes file paths in Windows environments that contain things that the Shell interprets as escape sequences.
	 * @param path a file path
	 * @return fixed path
	 */
	public static String handleShellEscapeProcessing(String path) {
		if (path == null) {
			return null;
		}
		char[] escaped = new char[] { 'r', 't', 'n', 'f', 'u' };
		for (char c : escaped) {
			path = path.replace("\\" + c, "\\\\" + c);
		}
		return path;
	}

}
