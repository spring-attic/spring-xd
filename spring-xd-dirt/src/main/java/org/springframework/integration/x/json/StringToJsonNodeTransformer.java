/*
 * Copyright 2002-2013 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.integration.x.json;

import java.io.IOException;

import org.springframework.integration.transformer.MessageTransformationException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Transforms a String to a {@link JsonNode}.
 * 
 * @author David Turanski
 */
public class StringToJsonNodeTransformer {

	private ObjectMapper mapper = new ObjectMapper();

	public JsonNode transform(Object json) {
		if (json instanceof JsonNode) {
			return (JsonNode) json;
		}
		try {
			return mapper.readTree((String) json);
		}
		catch (JsonParseException e) {
			throw new MessageTransformationException("unable to parse input: " + e.getMessage(), e);
		}
		catch (IOException e) {
			throw new MessageTransformationException("unable to save json parser: " + e.getMessage(), e);
		}
	}
}
