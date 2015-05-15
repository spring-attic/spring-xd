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

package org.springframework.xd.dirt.modules.metadata;

import java.util.Arrays;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;

/**
 *
 * @author Gunnar Hillert
 * @since 1.2
 */

public enum FileReadingMode {

	REF("ref", "use-ref"),
	TEXT_LINE("textLine", "use-contents-with-split"),
	FILE_AS_BYTES("fileAsBytes", "use-contents");

	private String key;

	private String profile;

	/**
	 * Constructor.
	 *
	 */
	FileReadingMode(final String key, final String profile) {
		this.key = key;
		this.profile = profile;
	}

	public String getKey() {
		return key;
	}

	public String getProfile() {
		return profile;
	}

	private static Function<FileReadingMode, String> func = new Function<FileReadingMode, String>() {

		@Override
		public String apply(FileReadingMode input) {
			return input.key;
		}
	};

	public static FileReadingMode fromKey(String key) {

		Assert.hasText(key, "FileReadingMode key must neither be null nor empty.");

		for (FileReadingMode fileReadingMode : FileReadingMode.values()) {
			if (fileReadingMode.getKey().equalsIgnoreCase(key)) {
				return fileReadingMode;
			}
		}

		throw new IllegalArgumentException(String.format(
				"Not a valid mode '%s'. Acceptable values are: %s",
				key,
				StringUtils.collectionToCommaDelimitedString(Collections2.transform(
						Arrays.asList(FileReadingMode.values()), func))));
	}

}
