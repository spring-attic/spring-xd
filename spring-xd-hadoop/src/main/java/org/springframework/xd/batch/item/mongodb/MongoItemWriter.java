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

package org.springframework.xd.batch.item.mongodb;

import java.util.List;

import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.mongodb.core.MongoOperations;

/**
 * 
 * @author Mark Pollack
 */
public class MongoItemWriter implements ItemWriter<Object>, InitializingBean {

	private MongoOperations mongoOperations;

	private String collectionName = "/data";

	public String getCollectionName() {
		return collectionName;
	}

	public void setCollectionName(String collectionName) {
		this.collectionName = collectionName;
	}

	public MongoItemWriter(MongoOperations mongoOperations) {
		this.mongoOperations = mongoOperations;
	}

	@Override
	public void write(List<? extends Object> items) throws Exception {
		mongoOperations.insert(items, collectionName);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (mongoOperations.collectionExists(collectionName) == false)
		{
			mongoOperations.createCollection(collectionName);
		}
	}

}
