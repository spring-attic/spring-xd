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

import org.springframework.xd.module.options.spi.ModuleOption;
import org.springframework.xd.module.options.spi.ModulePlaceholders;


/**
 * An option class to mix-in when reading from MongoDB. 
 *
 * @author Abhinav Gandhi
 */
public abstract class FromMongoDbOptionMixin {

	private String databaseName = "xd";

	private String host = "localhost";

	private int port = 27017;

	private String collectionName;

	private String username = "";

	private String password = "";
	
	private String query = "{}";
	
	private String authenticationDatabaseName = "";
	
	private int pollRate = 1000;
	
	private int maxMessages = 1;
	
	/**
	 * Has {@code collectionName} default to ${xd.job.name}.  
	 */
	public static class Job extends FromMongoDbOptionMixin {

		public Job() {
			super(ModulePlaceholders.XD_JOB_NAME);
		}
	}

	/**
	 * Has {@code collectionName} default to ${xd.stream.name}.  
	 */
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

	@ModuleOption("the MongoDB database name")
	public void setDatabaseName(String databaseName) {
		this.databaseName = databaseName;
	}

	@ModuleOption("the MongoDB host to connect to")
	public void setHost(String host) {
		this.host = host;
	}

	@ModuleOption("the MongoDB port to connect to")
	public void setPort(int port) {
		this.port = port;
	}

	@ModuleOption("the MongoDB password used for connecting")
	public void setPassword(String password) {
		this.password = password;
	}

	@ModuleOption("the MongoDB username used for connecting")
	public void setUsername(String username) {
		this.username = username;
	}

	@ModuleOption("the MongoDB authentication database used for connecting")
	public void setAuthenticationDatabaseName(String authenticationDatabaseName) {
		this.authenticationDatabaseName = authenticationDatabaseName;
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

	public String getPassword() {
		return password;
	}

	public String getUsername() {
		return username;
	}

	public String getAuthenticationDatabaseName() {
		return authenticationDatabaseName;
	}

	// @NotBlank
	public String getCollectionName() {
		return this.collectionName;
	}

	@NotBlank
	public String getDatabaseName() {
		return this.databaseName;
	}

	@NotBlank
	public String getHost() {
		return this.host;
	}

	@Range(min = 0, max = 65535)
	public int getPort() {
		return this.port;
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
