/*
 * Copyright 2009-2013 the original author or authors.
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

package org.springframework.xd.shell.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.util.Assert;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Some JSON helper utilities.
 * 
 * @author Thomas Risberg
 * @author Gunnar Hillert
 * @since 1.0
 * 
 */
public final class JsonUtil {

	private static final Logger logger = LoggerFactory.getLogger(JsonUtil.class);

	private final static ObjectMapper mapper = new ObjectMapper();
	static {
		mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
	}

	/**
	 * Prevent instantiation.
	 */
	private JsonUtil() {
		throw new AssertionError();
	}

	public static Map<String, Object> convertJsonToMap(String json) {
		Map<String, Object> retMap = new HashMap<String, Object>();
		if (json != null) {
			try {
				retMap = mapper.readValue(json, new TypeReference<Map<String, Object>>() {
				});
			}
			catch (IOException e) {
				logger.warn("Error while reading Java Map from JSON response: " + json, e);
			}
		}
		return retMap;
	}

	public static List<String> convertJsonToList(String json) {
		List<String> retList = new ArrayList<String>();
		if (json != null) {
			try {
				retList = mapper.readValue(json, new TypeReference<List<String>>() {
				});
			}
			catch (IOException e) {
				logger.warn("Error while reading Java List from JSON response: " + json, e);
			}
		}
		return retList;
	}

	public static String convertToJson(Object value) {

		Assert.notNull(value, "The object to convert must not be null.");

		if (mapper.canSerialize(value.getClass())) {
			try {
				return mapper.writeValueAsString(value);
			}
			catch (IOException e) {
				logger.warn("Error while serializing " + value + " to JSON", e);
				return null;
			}
		}
		else {
			throw new IllegalArgumentException("Value of type " + value.getClass().getName()
					+ " can not be serialized to JSON.");
		}
	}

}
