
package org.springframework.xd.test.jdbc;

import java.sql.Connection;

import org.springframework.xd.integration.util.XdEnvironment;
import org.springframework.xd.shell.command.fixtures.JdbcSink;
import org.springframework.xd.test.AbstractExternalResourceTestSupport;

/**
 * Provides the jdbc acceptance test the ability to be ignored if the Database is not available, 
 * or the Jdbc environment variables are missing.
 * 
 * @author Glenn Renfro
 */
public class JdbcTestSupport extends AbstractExternalResourceTestSupport<JdbcSink> {

	public JdbcTestSupport() {
		super("JDBC");
	}

	public Connection conn;

	@Override
	protected void obtainResource() throws Exception {
		resource = new JdbcSink();
		XdEnvironment environment = new XdEnvironment();
		if (environment.getJdbcUrl() == null || environment.getJdbcDatabase() == null
				|| environment.getJdbcDriver() == null) {
			throw new Exception("Required Env Variables Missing.");
		}
		resource.url(environment.getJdbcUrl()).driver(environment.getJdbcDriver()).database(
				environment.getJdbcDatabase());
		if (environment.getJdbcUsername() != null) {
			resource.username(environment.getJdbcUsername());
		}
		if (environment.getJdbcPassword() != null) {
			resource.password(environment.getJdbcPassword());
		}
		resource.start();
		conn = resource.getJdbcTemplate().getDataSource().getConnection();
	}

	@Override
	protected void cleanupResource() throws Exception {
		if (conn != null) {
			conn.close();
		}
	}

}
