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

import org.apache.log4j.EnhancedPatternLayout;
import org.apache.log4j.helpers.PatternParser;

/**
 * Extension of Log4j's {@link org.apache.log4j.EnhancedPatternLayout}
 * which supports logging of the "Implementation-Version" string
 * obtained from the manifest of the jar that
 * {@link org.springframework.xd.dirt.util.logging.VersionPatternParser}
 * is loaded from.
 * <p>
 * To include this version string in log output, add {@code %v}
 * to the logging conversion pattern string.
 *
 * @see org.springframework.xd.dirt.util.logging.VersionPatternLayout
 * @see org.springframework.xd.dirt.util.logging.VersionPatternParser
 *
 * @author Patrick Peralta
 */
public class VersionPatternLayout extends EnhancedPatternLayout {

	@Override
	protected PatternParser createPatternParser(String pattern) {
		return new VersionPatternParser(pattern);
	}

}
