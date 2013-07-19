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

package org.springframework.xd.jdbc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.integration.json.JsonToObjectTransformer;
import org.springframework.util.StringUtils;


/**
 * Transforms a payload to a Map representation based on provided column names.
 *
 * TODO: Right now it simply supports full payload under a 'payload' key or a JSON string with values stored with keys
 * corresponding to the column names specified. This could be expanded for additional payload types
 *
 * @author Thomas Risberg
 * @since 1.0
 */
@SuppressWarnings("rawtypes")
public class JdbcMessagePayloadTransformer extends JsonToObjectTransformer<Map> {
	
	private List<String> columnNames = new ArrayList<String>();
	
	public JdbcMessagePayloadTransformer() {
		super(Map.class);
	}

	public JdbcMessagePayloadTransformer(Class<Map> targetClass) {
		super(targetClass);
	}

	public String getColumns() {
		StringBuilder columns = new StringBuilder();
		for (String column : columnNames) {
			if (columns.length() > 0) {
				columns.append(", ");
			}
			columns.append(column);
		}
		return columns.toString();
	}

	public String getValues() {
		StringBuilder values = new StringBuilder();
		for (String column : columnNames) {
			if (values.length() > 0) {
				values.append(", ");
			}
			values.append(":payload[" + column + "]");
		}
		return values.toString();
	}

	public void setColumnNames(String columnNames) {
		String[] names = StringUtils.tokenizeToStringArray(columnNames, ",");
		for (String name : names) {
			this.columnNames.add(name.trim());
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	protected Map<String, Object> transformPayload(String payload)
			throws Exception {
		if (columnNames.size() == 1 && "payload".equals(columnNames.get(0))) {
			Map<String, Object> fullPayload = new HashMap<String, Object>();
			fullPayload.put("payload", payload);
			return fullPayload;
		}
		Map<String, Object> payloadMap = super.transformPayload(payload);
		for (String key : payloadMap.keySet()) {
			Object o = payloadMap.get(key);
			if (o != null && !(o instanceof String || o instanceof Number)) {
				payloadMap.put(key, o.toString());
			}
		}
		return payloadMap;
	}

}
