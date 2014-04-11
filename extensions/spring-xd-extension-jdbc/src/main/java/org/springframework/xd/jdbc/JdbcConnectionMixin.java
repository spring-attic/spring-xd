
package org.springframework.xd.jdbc;

import org.springframework.xd.module.options.spi.ModuleOption;

/**
 * Basic JDBC module/job options.
 * 
 * @author Luke Taylor
 */
public class JdbcConnectionMixin {

	protected String driverClass = "org.hsqldb.jdbc.JDBCDriver";

	protected String password = "";

	protected String url = "jdbc:hsqldb:hsql://localhost:9101/xdjob";

	protected String username = "sa";

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

}
