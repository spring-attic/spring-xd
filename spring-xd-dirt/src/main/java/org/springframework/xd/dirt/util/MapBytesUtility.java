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

package org.springframework.xd.dirt.util;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

/**
 * Utility to convert {@link Map string key/value pairs} to/from byte arrays containing JSON strings. By default the
 * JSON library encodes to UTF-8.
 * 
 * @author Patrick Peralta
 */
public class MapBytesUtility {

	/**
	 * Serializer from map to JSON string in a byte array.
	 */
	private final ObjectWriter writer;

	/**
	 * Deserializer from JSON string in a byte array to a map.
	 */
	private final ObjectReader reader;

	/**
	 * Construct a MapBytesUtility.
	 */
	public MapBytesUtility() {
		ObjectMapper mapper = new ObjectMapper();
		writer = mapper.writer();
		reader = mapper.reader(Map.class);
	}

	/**
	 * Convert a map of string key/value pairs to a JSON string in a byte array.
	 * 
	 * @param map map to convert
	 * 
	 * @return byte array
	 */
	public byte[] toByteArray(Map<String, String> map) {
		try {
			return writer.writeValueAsBytes(map);
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Convert a byte array containing a JSON string to a map of key/value pairs.
	 * 
	 * @param bytes byte array containing the key/value pair strings
	 * 
	 * @return a new map instance containing the key/value pairs
	 */
	public Map<String, String> toMap(byte[] bytes) {
		if (bytes == null || bytes.length == 0) {
			return Collections.emptyMap();
		}
		try {
			return reader.readValue(bytes);
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
