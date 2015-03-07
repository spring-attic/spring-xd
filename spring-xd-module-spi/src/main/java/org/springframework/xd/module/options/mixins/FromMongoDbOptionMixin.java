/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.xd.module.options.mixins;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.Range;

import org.springframework.xd.module.options.spi.Mixin;
import org.springframework.xd.module.options.spi.ModuleOption;
import org.springframework.xd.module.options.spi.ModulePlaceholders;


/**
 * An option class to mix-in when reading from MongoDB. 
 *
 * @author Abhinav Gandhi
 */
public abstract class FromMongoDbOptionMixin {

	private String query = "{}";

	private String collectionName;

	private int pollRate = 1000;
	
	private int maxMessages = 1;
	
	/**
	 * Has {@code collectionName} default to ${xd.job.name}.  
	 */
	@Mixin(MongoDbConnectionMixin.class)
	public static class Job extends FromMongoDbOptionMixin {

		public Job() {
			super(ModulePlaceholders.XD_JOB_NAME);
		}
	}

	/**
	 * Has {@code collectionName} default to ${xd.stream.name}.  
	 */
	@Mixin(MongoDbConnectionMixin.class)
	public static class Stream extends FromMongoDbOptionMixin {

		public Stream() {
			super(ModulePlaceholders.XD_STREAM_NAME);
		}
	}

	/**
	 * Subclasses should provide a default value for collectionName. 
	 */
	protected FromMongoDbOptionMixin(String collectionName) {
		this.collectionName = collectionName;
	}

	@ModuleOption("the MongoDB collection to store")
	public void setCollectionName(String collectionName) {
		this.collectionName = collectionName;
	}

	@ModuleOption("the rate at which to poll for data")
	public void setPollRate(int pollRate) {
		this.pollRate = pollRate;
	}
	
	@ModuleOption("the maximum number of messages to get at a time")
	public void setMaxMessages(int maxMessages) {
		this.maxMessages = maxMessages;
	}
	
	@ModuleOption("the query to make to the mongo db")
	public void setQuery(String query) {
		this.query = query;
	}

	// @NotBlank
	public String getCollectionName() {
		return this.collectionName;
	}

	public int getPollRate() {
		return this.pollRate;
	}
	
	public int getMaxMessages() {
		return this.maxMessages;
	}
	
	public String getQuery() {
		return this.query;
	}

}
