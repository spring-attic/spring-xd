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

package org.springframework.xd.dirt.util.logging;

import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.log4j.helpers.FormattingInfo;
import org.apache.log4j.helpers.PatternConverter;
import org.apache.log4j.spi.LoggingEvent;

import org.springframework.util.StringUtils;

/**
 * Implementation of {@link org.apache.log4j.helpers.PatternConverter}
 * that returns the "Implementation-Version" string obtained from the
 * manifest of the jar that this class was loaded from.
 * <p>
 * If this class was <b>not</b> loaded from a jar file or if
 * the manifest cannot be loaded, a default version string
 * of {@code unknown} is returned.
 *
 * @see org.springframework.xd.dirt.util.logging.VersionPatternLayout
 * @see org.springframework.xd.dirt.util.logging.VersionPatternParser
 *
 * @author Patrick Peralta
 */
public class VersionPatternConverter extends PatternConverter {
	private final String version;

	public VersionPatternConverter() {
		this.version = loadVersion();
	}

	public VersionPatternConverter(FormattingInfo fi) {
		super(fi);
		this.version = loadVersion();
	}

	/**
	 * Load the manifest for the jar file this class was packaged in
	 * and return the "Implementation-Version" field of the manifest.
	 * If this class was not loaded from a jar or if the manifest
	 * cannot be loaded, a default value of {@code unknown} is returned.
	 *
	 * @return value of "Implementation-Version" field from manifest
	 */
	private String loadVersion() {
		String version = "unknown";
		try {
			Class clz = getClass();
			String className = clz.getSimpleName() + ".class";
			String classPath = clz.getResource(className).toString();
			if (classPath.startsWith("jar")) {
				String manifestPath = classPath.substring(0, classPath.lastIndexOf('!') + 1) +
						"/META-INF/MANIFEST.MF";
				Manifest manifest = new Manifest(new URL(manifestPath).openStream());
				Attributes attributes = manifest.getMainAttributes();
				String value = attributes.getValue("Implementation-Version");
				if (StringUtils.hasText(value)) {
					version = value;
				}
			}
		}
		catch (Exception e) {
			// since this is used to initialize the logging framework,
			// the error cannot be logged via the logging framework
			System.out.println("Could not determine version of Spring XD via manifest");
			e.printStackTrace();
		}

		return version;
	}

	@Override
	protected String convert(LoggingEvent event) {
		return this.version;
	}

}
