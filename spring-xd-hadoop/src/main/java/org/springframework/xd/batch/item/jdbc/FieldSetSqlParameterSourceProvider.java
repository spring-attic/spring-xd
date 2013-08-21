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

package org.springframework.xd.batch.item.jdbc;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.springframework.batch.item.database.ItemSqlParameterSourceProvider;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

/**
 * An {@link ItemSqlParameterSourceProvider} that will derive parameters from a {@link FieldSet}.
 * 
 * @author Mark Pollack
 */
public class FieldSetSqlParameterSourceProvider implements ItemSqlParameterSourceProvider<FieldSet> {

	@Override
	public SqlParameterSource createSqlParameterSource(FieldSet item) {
		MapSqlParameterSource source = new MapSqlParameterSource();

		Properties props = item.getProperties();
		Set<String> keys = new HashSet<String>(props.stringPropertyNames());
		for (String key : keys) {
			source.addValue(key, props.get(key));
		}

		return source;
	}

}
