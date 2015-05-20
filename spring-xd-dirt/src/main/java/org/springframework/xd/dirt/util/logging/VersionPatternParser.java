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

import org.apache.log4j.helpers.PatternParser;

/**
 * Extension of Log4j's {@link org.apache.log4j.helpers.PatternParser}
 * that recognizes the pattern {@code %v} which is used to log the
 * "Implementation-Version" string obtained from the manifest of the
 * jar that {@link org.springframework.xd.dirt.util.logging.VersionPatternParser}
 * is loaded from.
 *
 * @see org.springframework.xd.dirt.util.logging.VersionPatternConverter
 * @see org.springframework.xd.dirt.util.logging.VersionPatternLayout
 * @author Patrick Peralta
 */
public class VersionPatternParser extends PatternParser {

	private static final char VERSION_CHAR = 'v';

	public VersionPatternParser(String pattern) {
		super(pattern);
	}

	@Override
	protected void finalizeConverter(char c) {
		if (c == VERSION_CHAR) {
			currentLiteral.setLength(0);
		}
		else {
			super.finalizeConverter(c);
		}
	}

}
