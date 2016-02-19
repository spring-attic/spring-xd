/*
 * Copyright 2013-2016 the original author or authors.
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

package org.springframework.xd.dirt.util;

import java.io.InputStreamReader;

import org.apache.commons.lang.StringUtils;

import org.springframework.util.FileCopyUtils;

/**
 * Provides utilities for rendering graphical console output.
 *
 * @author Gunnar Hillert
 * @author David Turanski
 * @author Gary Russell
 * @since 1.0
 */
public final class BannerUtils {

	private static final String LINE_SEPARATOR = System
			.getProperty("line.separator");

	/** Prevent instantiation. */
	private BannerUtils() {
		throw new AssertionError();
	}

	/**
	 * Retrieves the ASCII resource "banner.txt" from the classpath. Will also use the version number from
	 * "MANIFEST.MF".
	 *
	 * Similar to {@link org.springframework.shell.plugin.support.DefaultBannerProvider}.
	 *
	 * @return String representing the Banner.
	 */
	private static String getBanner() {
		final String banner = readBanner();
		final String version = StringUtils.rightPad(BannerUtils.getVersion(),
				33);
		final StringBuilder sb = new StringBuilder();
		sb.append(String.format(banner, version));
		sb.append(LINE_SEPARATOR);
		return sb.toString();
	}

	private static String readBanner() {
		try {
			return FileCopyUtils.copyToString(
					new InputStreamReader(BannerUtils.class
							.getResourceAsStream("banner.txt"))).replaceAll(
					"(\\r|\\n)+", LINE_SEPARATOR);
		}
		catch (Exception e) {
			throw new IllegalStateException("Could not read banner.txt");
		}
	}

	public static String getVersion() {
		final String version = XdUtils.getSpringXdVersion();
		return (version != null ? version : "Unknown Version");
	}

	public static String displayBanner(String containerName,
			String additionalMessage) {
		StringBuffer sb = new StringBuffer(LINE_SEPARATOR);
		sb.append(getBanner())
				.append(StringUtils.isEmpty(additionalMessage) ? ""
						: additionalMessage)
				.append(LINE_SEPARATOR)
				.append(StringUtils.isEmpty(containerName) ? ""
						: "Started : " + containerName)
				.append(LINE_SEPARATOR)
				.append("Documentation: http://docs.spring.io/spring-xd/docs/current/reference/html/")
				.append(LINE_SEPARATOR);
		return sb.toString();
	}

}
