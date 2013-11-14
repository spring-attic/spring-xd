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

package org.springframework.integration.x.gemfire;

import org.springframework.integration.transformer.MessageTransformationException;

import com.gemstone.gemfire.pdx.JSONFormatter;
import com.gemstone.gemfire.pdx.PdxInstance;
import com.gemstone.org.json.JSONException;
import com.gemstone.org.json.JSONObject;

/**
 * @author David Turanski
 * 
 */
public class JsonStringToObjectTransformer {

	public PdxInstance toObject(String json) {
		JSONObject jsonObject = null;
		try {
			jsonObject = new JSONObject(json);
		}
		catch (JSONException e) {
			throw new MessageTransformationException(e.getMessage());
		}
		return JSONFormatter.fromJSON(jsonObject.toString());
	}

	public String toString(Object obj) {
		if (obj == null) {
			return null;
		}
		if (obj instanceof PdxInstance) {
			String json = JSONFormatter.toJSON((PdxInstance) obj);
			// de-pretty
			return json.replaceAll("\\r\\n\\s*", "").replaceAll("\\n\\s*", "").replaceAll("\\s*:\\s*", ":").trim();
		}
		return obj.toString();
	}
}
