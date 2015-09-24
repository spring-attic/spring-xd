/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.xd.cassandra.mixins;

import javax.validation.constraints.AssertFalse;
import javax.validation.constraints.NotNull;

import org.springframework.cassandra.config.CassandraCqlClusterFactoryBean;
import org.springframework.cassandra.config.CompressionType;
import org.springframework.util.StringUtils;
import org.springframework.xd.module.options.spi.ModuleOption;
import org.springframework.xd.module.options.spi.ModulePlaceholders;

import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.Range;

/**
 * @author Artem Bilan
 */
public class CassandraConnectionMixin {

	private String contactPoints = CassandraCqlClusterFactoryBean.DEFAULT_CONTACT_POINTS;

	private int port = CassandraCqlClusterFactoryBean.DEFAULT_PORT;

	private String keyspace = ModulePlaceholders.XD_STREAM_NAME;

	private String username;

	private String password;

	private String initScript;

	private String[] entityBasePackages = new String[0];

	private CompressionType compressionType = CompressionType.NONE;

	private boolean metricsEnabled = CassandraCqlClusterFactoryBean.DEFAULT_METRICS_ENABLED;

	@ModuleOption("the comma-delimited string of the hosts to connect to Cassandra")
	public void setContactPoints(String contactPoints) {
		this.contactPoints = contactPoints;
	}

	@ModuleOption("the port to use to connect to the Cassandra host")
	public void setPort(int port) {
		this.port = port;
	}

	@ModuleOption("the keyspace name to connect to")
	public void setKeyspace(String keyspace) {
		this.keyspace = keyspace;
	}

	@ModuleOption("the username for connection")
	public void setUsername(String username) {
		this.username = username;
	}

	@ModuleOption("the password for connection")
	public void setPassword(String password) {
		this.password = password;
	}

	@ModuleOption("the path to file with CQL scripts (delimited by ';') to initialize keyspace schema")
	public void setInitScript(String initScript) {
		this.initScript = initScript;
	}

	@ModuleOption("the base packages to scan for entities annotated with Table annotations")
	public void setEntityBasePackages(String[] entityBasePackages) {
		this.entityBasePackages = entityBasePackages;
	}

	@ModuleOption("the compression to use for the transport")
	public void setCompressionType(CompressionType compressionType) {
		this.compressionType = compressionType;
	}

	@ModuleOption("enable/disable metrics collection for the created cluster")
	public void setMetricsEnabled(boolean metricsEnabled) {
		this.metricsEnabled = metricsEnabled;
	}

	@NotBlank
	public String getContactPoints() {
		return this.contactPoints;
	}

	@Range(min = 0, max = 65535)
	public int getPort() {
		return this.port;
	}

	public String getKeyspace() {
		return this.keyspace;
	}

	public String getUsername() {
		return this.username;
	}

	public String getPassword() {
		return this.password;
	}

	public String getInitScript() {
		return this.initScript;
	}

	public String[] getEntityBasePackages() {
		return this.entityBasePackages;
	}

	@NotNull
	public CompressionType getCompressionType() {
		return this.compressionType;
	}

	public boolean isMetricsEnabled() {
		return this.metricsEnabled;
	}

	@AssertFalse(message = "both 'username' and 'password' are required or neither one")
	private boolean isInvalid() {
		return StringUtils.hasText(this.username) ^ StringUtils.hasText(this.password);
	}

}
