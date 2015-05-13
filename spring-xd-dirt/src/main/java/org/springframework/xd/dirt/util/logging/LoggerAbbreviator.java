/*
 * Copyright 2015 the original author or authors.
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
package org.springframework.xd.dirt.util.logging;

import ch.qos.logback.classic.pattern.Abbreviator;

/**
 * @author David Turanski
 */
public class LoggerAbbreviator implements Abbreviator {
	private final int targetLength;

	LoggerAbbreviator(int targetLength) {
		this.targetLength = targetLength;
	}

	public String abbreviate(String fqClassName) {
		String[] toks = fqClassName.split("\\.");
		if (toks.length <= targetLength) {
			return fqClassName;
		}
		else {
			StringBuilder sb = new StringBuilder();
			for (int i = toks.length - targetLength; i < toks.length; i++) {
				sb.append(toks[i]);
				if (i < toks.length - 1) {
					sb.append(".");
				}
			}
			return sb.toString();
		}
	}
}

