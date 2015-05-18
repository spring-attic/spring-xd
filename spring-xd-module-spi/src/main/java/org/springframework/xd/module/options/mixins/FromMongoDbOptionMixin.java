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

import org.springframework.xd.module.options.spi.Mixin;
import org.springframework.xd.module.options.spi.ModuleOption;
import org.springframework.xd.module.options.spi.ModulePlaceholders;
import org.springframework.xd.module.options.spi.ProfileNamesProvider;


/**
 * An option class to mix-in when reading from MongoDB.
 *
 * @author Abhinav Gandhi
 * @author Gary Russell
 */
public abstract class FromMongoDbOptionMixin implements ProfileNamesProvider {

	private static final String[] USE_SPLITTER = new String[] { "use-splitter" };

	private static final String[] DONT_USE_SPLITTER = new String[] { "dont-use-splitter" };


	private String query = "{}";

	private String collectionName;

	private int fixedDelay = 1000;

	private boolean split = true;


	/**
	 * Has {@code collectionName} default to ${xd.job.name}.
	 */
	@Mixin({ MongoDbConnectionMixin.class, PeriodicTriggerMixin.class, MaxMessagesDefaultOneMixin.class })
	public static class Job extends FromMongoDbOptionMixin {

		public Job() {
			super(ModulePlaceholders.XD_JOB_NAME);
		}
	}

	/**
	 * Has {@code collectionName} default to ${xd.stream.name}.
	 */
	@Mixin({ MongoDbConnectionMixin.class, PeriodicTriggerMixin.class, MaxMessagesDefaultOneMixin.class })
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

	@ModuleOption("the MongoDB collection to read from")
	public void setCollectionName(String collectionName) {
		this.collectionName = collectionName;
	}

	@ModuleOption("the time delay between polls for data, expressed in TimeUnits (seconds by default)")
	public void setFixedDelay(int fixedDelay) {
		this.fixedDelay = fixedDelay;
	}

	@ModuleOption("the query to make to the mongo db")
	public void setQuery(String query) {
		this.query = query;
	}

	// @NotBlank
	public String getCollectionName() {
		return this.collectionName;
	}

	public int getFixedDelay() {
		return this.fixedDelay;
	}

	public String getQuery() {
		return this.query;
	}

	public boolean isSplit() {
		return split;
	}

	@ModuleOption("whether to split the query result as individual messages")
	public void setSplit(boolean split) {
		this.split = split;
	}

	@Override
	public String[] profilesToActivate() {
		return split ? USE_SPLITTER : DONT_USE_SPLITTER;
	}


}
