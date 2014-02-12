package org.springframework.xd.jdbc;

import org.springframework.xd.module.options.spi.ModuleOption;

/**
 * Basic JDBC module/job options.
 *
 * @author Luke Taylor
 */
public abstract class AbstractJdbcOptionsMetadata {
	protected String driverClass;
	protected String password;
	protected String url;
	protected String username;
	protected String configProperties;

	@ModuleOption("the JDBC driver to use")
	public void setDriverClass(String driverClass) {
		this.driverClass = driverClass;
	}

	@ModuleOption("the JDBC password")
	public void setPassword(String password) {
		this.password = password;
	}

	@ModuleOption("the JDBC URL for the database")
	public void setUrl(String url) {
		this.url = url;
	}

	@ModuleOption("the JDBC username")
	public void setUsername(String username) {
		this.username = username;
	}

	@ModuleOption("the name of the properties file (in /config) used to override database settings")
	public void setConfigProperties(String configProperties) {
		this.configProperties = configProperties;
	}

	public String getDriverClass() {
		return driverClass;
	}

	public String getPassword() {
		return password;
	}

	public String getUrl() {
		return url;
	}

	public String getUsername() {
		return username;
	}

	public String getConfigProperties() {
		return configProperties;
	}
}
