/*
 * Copyright 2014-2015 the original author or authors.
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

package org.springframework.xd.dirt.modules.metadata;

import org.springframework.xd.module.options.spi.ModuleOption;


/**
 * Mixin for Redis connection options.
 *
 * @author Ilayaperumal Gopinathan
 * @author Mark Pollack
 */
public class RedisConnectionMixin {

	private String hostname = "localhost";

	private int port = 6379;

	private String password = "";

	private int database = 0;

	private String sentinelMaster = "";

	private String sentinelNodes = "";

	private int maxIdle = 8;

	private int minIdle = 0;

	private int maxActive = 8;

	private int maxWait = -1;

	public String getHostname() {
		return hostname;
	}

	@ModuleOption("redis host name")
	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public int getPort() {
		return port;
	}

	@ModuleOption("redis port")
	public void setPort(int port) {
		this.port = port;
	}

	public String getPassword() {
		return password;
	}

	@ModuleOption("redis password")
	public void setPassword(String password) {
		this.password = password;
	}

	public int getDatabase() {
		return database;
	}

	@ModuleOption("database index used by the connection factory")
	public void setDatabase(int database) {
		this.database = database;
	}

	public String getSentinelMaster() {
		return sentinelMaster;
	}

	@ModuleOption("name of Redis master server")
	public void setSentinelMaster(String sentinelMaster) {
		this.sentinelMaster = sentinelMaster;
	}

	public String getSentinelNodes() {
		return sentinelNodes;
	}

	@ModuleOption("comma-separated list of host:port pairs")
	public void setSentinelNodes(String sentinelNodes) {
		this.sentinelNodes = sentinelNodes;
	}

	public int getMaxIdle() {
		return maxIdle;
	}

	@ModuleOption("max number of idle connections in the pool; "
			+ "a negative value indicates an unlimited number of idle connections")
	public void setMaxIdle(int maxIdle) {
		this.maxIdle = maxIdle;
	}

	public int getMinIdle() {
		return minIdle;
	}

	@ModuleOption("target for the minimum number of idle connections to maintain in the pool; "
			+ "only has an effect if it is positive")
	public void setMinIdle(int minIdle) {
		this.minIdle = minIdle;
	}

	public int getMaxActive() {
		return maxActive;
	}

	@ModuleOption("max number of connections that can be allocated by the pool at a given time; "
			+ "negative value for no limit")
	public void setMaxActive(int maxActive) {
		this.maxActive = maxActive;
	}

	public int getMaxWait() {
		return maxWait;
	}

	@ModuleOption("max amount of time (in milliseconds) a connection allocation should block "
			+ "before throwing an exception when the pool is exhausted; "
			+ "negative value to block indefinitely")
	public void setMaxWait(int maxWait) {
		this.maxWait = maxWait;
	}
}
