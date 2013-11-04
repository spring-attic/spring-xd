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

package org.springframework.xd.mongodb;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.core.convert.converter.Converter;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * A converter that maps from the batch {@link FieldSet} world to Mongo's {@link DBObject}. A field named {@code id}
 * will be mapped to Mongo's {@code _id}.
 * 
 * @author Mark Pollack
 */
public class FieldSetConverter implements Converter<FieldSet, DBObject> {

	@Override
	public DBObject convert(FieldSet fieldSet) {
		DBObject dbo = new BasicDBObject();
		Properties props = fieldSet.getProperties();
		Set<String> keys = new HashSet<String>(props.stringPropertyNames());
		for (String key : keys) {
			if (key.compareToIgnoreCase("id") == 0) {
				dbo.put("_id", props.get(key));
			}
			else {
				dbo.put(key, props.get(key));
			}
		}
		return dbo;
	}
}
