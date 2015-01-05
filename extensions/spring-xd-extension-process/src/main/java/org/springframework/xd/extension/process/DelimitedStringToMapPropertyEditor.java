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

package org.springframework.xd.extension.process;

import java.beans.PropertyEditorSupport;
import java.util.HashMap;
import java.util.Map;

import org.springframework.util.StringUtils;

/**
 * Converts a String of comma delimited name-value pairs to a Map.
 * Used here to set environment variables that are passed as a comma delimited String.
 *
 * @author David Turanski
 */
public class DelimitedStringToMapPropertyEditor extends PropertyEditorSupport {

	@Override
	@SuppressWarnings("unchecked")
	public void setAsText(String text) {
		Map<String, String> result = new HashMap<String, String>();
		if (!StringUtils.isEmpty(text)) {
			String[] nameValuePairs = StringUtils.commaDelimitedListToStringArray(text);

			for (String nameValuePair : nameValuePairs) {
				String[] nameAndValue = nameValuePair.split("=");
				if (nameAndValue.length != 2) {
					throw new RuntimeException("Invalid name-value pair: '" + text + "'");
				}
				result.put(nameAndValue[0], nameAndValue[1]);
			}
		}
		setValue(result);
	}
}
