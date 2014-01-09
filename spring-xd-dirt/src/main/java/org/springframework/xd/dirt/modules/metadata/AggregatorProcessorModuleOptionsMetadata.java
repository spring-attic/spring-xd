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

package org.springframework.xd.dirt.modules.metadata;

import static org.springframework.xd.dirt.modules.metadata.AggregatorProcessorModuleOptionsMetadata.StoreKind.memory;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;

import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.Range;

import org.springframework.xd.module.options.spi.ModuleOption;
import org.springframework.xd.module.options.spi.ProcessorModuleOptionsMetadataSupport;
import org.springframework.xd.module.options.spi.ProfileNamesProvider;
import org.springframework.xd.module.options.spi.ValidationGroupsProvider;


/**
 * Describes options to the {@code aggregator} processor module.
 * 
 * @author Eric Bottard
 */
public class AggregatorProcessorModuleOptionsMetadata extends ProcessorModuleOptionsMetadataSupport implements
		ProfileNamesProvider, ValidationGroupsProvider {

	private String correlation;

	private String release;

	private int count = 50;

	private String aggregation = "#this.![payload]";

	private int timeout = 50000;

	private StoreKind store = memory;

	public static enum StoreKind {
		memory, jdbc, redis;
	}

	// Redis store
	private String hostname = "localhost";

	private int port = 6379;

	// Jdbc store
	private boolean initializeDatabase;

	private String dbkind;

	private String driverClass;

	private String url;

	private String username;

	// This is used by both redis & jdbc
	private String password = "";

	private static interface RedisStore extends Default {
	}

	private static interface JdbcStore extends Default {
	}

	@ModuleOption("how to correlate messages (SpEL expression against each message)")
	public void setCorrelation(String correlation) {
		this.correlation = correlation;
	}

	@ModuleOption("when to release messages (SpEL expression against a collection of messages accumulated so far)")
	public void setRelease(String release) {
		this.release = release;
	}

	@ModuleOption("the number of messages to group together before emitting a group")
	public void setCount(int count) {
		this.count = count;
	}

	@ModuleOption("how to construct the aggregated message (SpEL expression against a collection of messages)")
	public void setAggregation(String aggregation) {
		this.aggregation = aggregation;
	}

	@ModuleOption("the delay (ms) after which messages should be released, even if the completion criteria is not met")
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	@ModuleOption("the kind of store to use to retain messages")
	public void setStore(StoreKind store) {
		this.store = store;
	}

	@ModuleOption("hostname of the redis instance to use as a store")
	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	@ModuleOption("port of the redis instance to use as a store")
	public void setPort(int port) {
		this.port = port;
	}

	@ModuleOption("whether to auto-create the database tables for the jdbc store")
	public void setInitializeDatabase(boolean initializeDatabase) {
		this.initializeDatabase = initializeDatabase;
	}

	@ModuleOption("which flavor of init scripts to use for the jdbc store (default attempts autodetection)")
	public void setDbkind(String dbkind) {
		this.dbkind = dbkind;
	}

	@ModuleOption("the jdbc driver to use when using the jdbc store")
	public void setDriverClass(String driverClass) {
		this.driverClass = driverClass;
	}

	@ModuleOption("the jdbc url to connect to when using the jdbc store")
	public void setUrl(String url) {
		this.url = url;
	}

	@ModuleOption("the username to use when using the jdbc store")
	public void setUsername(String username) {
		this.username = username;
	}

	@ModuleOption("the password to use when using the jdbc or redis store")
	public void setPassword(String password) {
		this.password = password;
	}


	public String getCorrelation() {
		return correlation;
	}

	@NotNull
	public String getRelease() {
		return release != null ? release : String.format("size() == %d", getCount());
	}

	@Min(0)
	public int getCount() {
		return count;
	}

	@NotNull
	public String getAggregation() {
		return aggregation;
	}

	@Min(0)
	public int getTimeout() {
		return timeout;
	}

	@NotNull
	public StoreKind getStore() {
		return store;
	}

	@NotNull(groups = RedisStore.class)
	public String getHostname() {
		return hostname;
	}

	@Range(min = 0, max = 65535, groups = RedisStore.class)
	@NotNull(groups = RedisStore.class)
	public Integer getPort() {
		return port;
	}


	public boolean isInitializeDatabase() {
		return initializeDatabase;
	}


	public String getDbkind() {
		return dbkind;
	}

	@NotBlank(groups = JdbcStore.class)
	public String getDriverClass() {
		return driverClass;
	}


	@NotBlank(groups = JdbcStore.class)
	public String getUrl() {
		return url;
	}


	@NotBlank(groups = JdbcStore.class)
	public String getUsername() {
		return username;
	}


	public String getPassword() {
		return password;
	}

	@Override
	public String[] profilesToActivate() {
		return new String[] { String.format("use-%s-store", store) };
	}

	@Override
	public Class<?>[] groupsToValidate() {
		switch (store) {
			case jdbc:
				return new Class<?>[] { JdbcStore.class };
			case redis:
				return new Class<?>[] { RedisStore.class };
			default:
				return new Class<?>[] { Default.class };
		}
	}
}
