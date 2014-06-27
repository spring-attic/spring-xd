/*
 * Copyright 2014 the original author or authors.
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

import org.springframework.xd.module.options.spi.ModuleOption;

/**
 * Basic JDBC module/job connection pool options.
 * 
 * @author Glenn Renfro
 */
public class JdbcConnectionPoolMixin {

	// Common Attributes
	protected int maxActive = 100;

	protected int maxIdle = 100;

	protected int minIdle = 10;

	protected int initialSize = 0;

	protected int maxWait = 30000;

	protected boolean testOnBorrow = false;

	protected boolean testOnReturn = false;

	protected boolean testWhileIdle = false;

	protected String validationQuery = null;

	protected String validatorClassName = null;

	protected int timeBetweenEvictionRunsMillis = 5000;

	protected int minEvictableIdleTimeMillis = 60000;

	protected boolean removeAbandoned = false;

	protected int removeAbandonedTimeout = 60;

	protected boolean logAbandoned = false;

	protected String connectionProperties = null;

	//Tomcat JDBC Enhanced Attributes

	protected String initSQL = null;

	protected String jdbcInterceptors = null;

	protected long validationInterval = 30000;

	protected boolean jmxEnabled = true;

	protected boolean fairQueue = true;

	protected int abandonWhenPercentageFull = 0;

	protected int maxAge = 0;

	protected boolean useEquals = true;

	protected int suspectTimeout = 0;

	protected boolean alternateUsernameAllowed = false;


	@ModuleOption(value = "maximum number of active connections that can be allocated from this pool at the same time", hidden = true)
	public void setMaxActive(int maxActive) {
		this.maxActive = maxActive;
	}

	@ModuleOption(value = "maximum number of connections that should be kept in the pool at all times", hidden = true)
	public void setMaxIdle(int maxIdle) {
		this.maxIdle = maxIdle;
	}

	@ModuleOption(value = "minimum number of established connections that should be kept in the pool at all times", hidden = true)
	public void setMinIdle(int minIdle) {
		this.minIdle = minIdle;
	}

	@ModuleOption(value = "initial number of connections that are created when the pool is started", hidden = true)
	public void setInitialSize(int initialSize) {
		this.initialSize = initialSize;
	}

	@ModuleOption(value = "maximum number of milliseconds that the pool will wait for a connection", hidden = true)
	public void setMaxWait(int maxWait) {
		this.maxWait = maxWait;
	}

	@ModuleOption(value = "indication of whether objects will be validated before being borrowed from the pool", hidden = true)
	public void setTestOnBorrow(boolean testOnBorrow) {
		this.testOnBorrow = testOnBorrow;
	}

	@ModuleOption(value = "indication of whether objects will be validated before being returned to the pool", hidden = true)
	public void setTestOnReturn(boolean testOnReturn) {
		this.testOnReturn = testOnReturn;
	}

	@ModuleOption(value = "indication of whether objects will be validated by the idle object evictor", hidden = true)
	public void setTestWhileIdle(boolean testWhileIdle) {
		this.testWhileIdle = testWhileIdle;
	}

	@ModuleOption(value = "sql query that will be used to validate connections from this pool", hidden = true)
	public void setValidationQuery(String validationQuery) {
		this.validationQuery = validationQuery;
	}

	@ModuleOption(value = "name of a class which implements the org.apache.tomcat.jdbc.pool.Validator", hidden = true)
	public void setValidatorClassName(String validatorClassName) {
		this.validatorClassName = validatorClassName;
	}

	@ModuleOption(value = "number of milliseconds to sleep between runs of the idle connection validation/cleaner thread", hidden = true)
	public void setTimeBetweenEvictionRunsMillis(int timeBetweenEvictionRunsMillis) {
		this.timeBetweenEvictionRunsMillis = timeBetweenEvictionRunsMillis;
	}

	@ModuleOption(value = "minimum amount of time an object may sit idle in the pool before it is eligible for eviction", hidden = true)
	public void setMinEvictableIdleTimeMillis(int minEvictableIdleTimeMillis) {
		this.minEvictableIdleTimeMillis = minEvictableIdleTimeMillis;
	}

	@ModuleOption(value = "flag to remove abandoned connections if they exceed the removeAbandonedTimout", hidden = true)
	public void setRemoveAbandoned(boolean removeAbandoned) {
		this.removeAbandoned = removeAbandoned;
	}

	@ModuleOption(value = "timeout in seconds before an abandoned connection can be removed", hidden = true)
	public void setRemoveAbandonedTimeout(int removeAbandonedTimeout) {
		this.removeAbandonedTimeout = removeAbandonedTimeout;
	}

	@ModuleOption(value = "flag to log stack traces for application code which abandoned a Connection", hidden = true)
	public void setLogAbandoned(boolean logAbandoned) {
		this.logAbandoned = logAbandoned;
	}

	@ModuleOption(value = "connection properties that will be sent to our JDBC driver when establishing new connections", hidden = true)
	public void setConnectionProperties(String connectionProperties) {
		this.connectionProperties = connectionProperties;
	}

	@ModuleOption(value = "custom query to be run when a connection is first created", hidden = true)
	public void setInitSQL(String initSQL) {
		this.initSQL = initSQL;
	}

	@ModuleOption(value = "semicolon separated list of classnames extending org.apache.tomcat.jdbc.pool.JdbcInterceptor", hidden = true)
	public void setJdbcInterceptors(String jdbcInterceptors) {
		this.jdbcInterceptors = jdbcInterceptors;
	}

	@ModuleOption(value = "avoid excess validation, only run validation at most at this frequency - time in milliseconds", hidden = true)
	public void setValidationInterval(long validationInterval) {
		this.validationInterval = validationInterval;
	}

	@ModuleOption(value = "register the pool with JMX or not", hidden = true)
	public void setJmxEnabled(boolean jmxEnabled) {
		this.jmxEnabled = jmxEnabled;
	}

	@ModuleOption(value = "set to true if you wish that calls to getConnection should be treated fairly in a true FIFO fashion", hidden = true)
	public void setFairQueue(boolean fairQueue) {
		this.fairQueue = fairQueue;
	}

	@ModuleOption(value = "connections that have timed out wont get closed and reported up unless the number of connections in use are above the percentage", hidden = true)
	public void setAbandonWhenPercentageFull(int abandonWhenPercentageFull) {
		this.abandonWhenPercentageFull = abandonWhenPercentageFull;
	}

	@ModuleOption(value = "time in milliseconds to keep this connection", hidden = true)
	public void setMaxAge(int maxAge) {
		this.maxAge = maxAge;
	}

	@ModuleOption(value = "true if you wish the ProxyConnection class to use String.equals", hidden = true)
	public void setUseEquals(boolean useEquals) {
		this.useEquals = useEquals;
	}

	@ModuleOption(value = "this simply logs the warning after timeout, connection remains", hidden = true)
	public void setSuspectTimeout(int suspectTimeout) {
		this.suspectTimeout = suspectTimeout;
	}

	@ModuleOption(value = "uses an alternate user name if connection fails", hidden = true)
	public void setAlternateUsernameAllowed(boolean alternateUsernameAllowed) {
		this.alternateUsernameAllowed = alternateUsernameAllowed;
	}


	public int getMaxActive() {
		return maxActive;
	}


	public int getMaxIdle() {
		return maxIdle;
	}


	public int getMinIdle() {
		return minIdle;
	}


	public int getInitialSize() {
		return initialSize;
	}


	public int getMaxWait() {
		return maxWait;
	}


	public boolean isTestOnBorrow() {
		return testOnBorrow;
	}


	public boolean isTestOnReturn() {
		return testOnReturn;
	}


	public boolean isTestWhileIdle() {
		return testWhileIdle;
	}


	public String getValidationQuery() {
		return validationQuery;
	}


	public String getValidatorClassName() {
		return validatorClassName;
	}


	public int getTimeBetweenEvictionRunsMillis() {
		return timeBetweenEvictionRunsMillis;
	}

	public int getMinEvictableIdleTimeMillis() {
		return minEvictableIdleTimeMillis;
	}


	public boolean isRemoveAbandoned() {
		return removeAbandoned;
	}


	public int getRemoveAbandonedTimeout() {
		return removeAbandonedTimeout;
	}


	public boolean isLogAbandoned() {
		return logAbandoned;
	}


	public String getConnectionProperties() {
		return connectionProperties;
	}


	public String getInitSQL() {
		return initSQL;
	}


	public String getJdbcInterceptors() {
		return jdbcInterceptors;
	}


	public long getValidationInterval() {
		return validationInterval;
	}


	public boolean isJmxEnabled() {
		return jmxEnabled;
	}


	public boolean isFairQueue() {
		return fairQueue;
	}


	public int getAbandonWhenPercentageFull() {
		return abandonWhenPercentageFull;
	}


	public int getMaxAge() {
		return maxAge;
	}


	public boolean isUseEquals() {
		return useEquals;
	}


	public int getSuspectTimeout() {
		return suspectTimeout;
	}


	public boolean isAlternateUsernameAllowed() {
		return alternateUsernameAllowed;
	}
}
