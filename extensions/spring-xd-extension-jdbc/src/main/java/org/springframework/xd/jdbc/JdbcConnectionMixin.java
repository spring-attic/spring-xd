
package org.springframework.xd.jdbc;

import org.springframework.xd.module.options.spi.ModuleOption;

/**
 * Basic JDBC module/job options.
 * 
 * @author Luke Taylor
 */
public class JdbcConnectionMixin {

	protected String driverClassName;

	protected String password;

	protected String url;

	protected String username;

	@ModuleOption("the JDBC driver to use")
	public void setDriverClassName(String driverClass) {
		this.driverClassName = driverClass;
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

	public String getDriverClassName() {
		return driverClassName;
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

}
