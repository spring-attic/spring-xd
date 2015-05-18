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

import org.springframework.core.convert.converter.Converter;
import org.springframework.xd.tuple.Tuple;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * @author Luke Taylor
 */
public class TupleWriteConverter implements Converter<Tuple, DBObject> {

	private String idField = null;

	/**
	 * Set the tuple field name which will be used as the Mongo _id. If not set, the MongoDriver will
	 * will assign a ObjectId with a generated value.
	 * 
	 * @param idField the name of the field to use as the identity in MongoDB.
	 */
	public void setIdField(String idField) {
		this.idField = idField;
	}

	@Override
	public DBObject convert(Tuple source) {
		DBObject dbo = new BasicDBObject();
		for (int i = 0; i < source.getFieldCount(); i++) {
			String name = source.getFieldNames().get(i);
			if (name.equals(idField)) {
				dbo.put("_id", source.getValue(i));
			}
			else {
				dbo.put(name, source.getValue(i));
			}
		}
		return dbo;
	}
}
