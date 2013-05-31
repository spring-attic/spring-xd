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

package org.springframework.xd.dirt.listener.util;

import org.apache.commons.lang.StringUtils;

import org.springframework.shell.support.util.FileUtils;
import org.springframework.shell.support.util.OsUtils;

/**
 * Provides utilities for rendering graphical console output.
 *
 * @author Gunnar Hillert
 * @since 1.0
 */
public final class BannerUtils {

	/** Prevent instantiation. */
	private BannerUtils() {
		throw new AssertionError();
	}

	/**
	 * Retrieves the ASCII resource "banner.txt" from the classpath. Will also
	 * use the version number from "MANIFEST.MF".
	 *
	 * Similar to
	 * {@link org.springframework.shell.plugin.support.DefaultBannerProvider}.
	 *
	 * @return String representing the Banner.
	 */
	public static String getBanner() {
		final String banner = FileUtils.readBanner(BannerUtils.class, "banner.txt");
		final String version = StringUtils.rightPad(BannerUtils.getVersion(), 33);
		final StringBuilder sb = new StringBuilder();
		sb.append(String.format(banner, version));
		sb.append(OsUtils.LINE_SEPARATOR);
		return sb.toString();
	}

	public static String getVersion() {
		final Package pkg = BannerUtils.class.getPackage();
		String version = null;
		if (pkg != null) {
			version = pkg.getImplementationVersion();
		}
		return (version != null ? version : "Unknown Version");
	}

}
