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

package org.springframework.xd.greenplum.support;

import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSource;

import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.util.StringUtils;

/**
 * Factory bean for configuring a {@link DataSource}. Needed to use
 * both command-line props and a control file.
 *
 * @author Janne Valkealahti
 */
public class GreenplumDataSourceFactoryBean extends AbstractFactoryBean<BasicDataSource> {

	private ControlFile controlFile;

	private String dbHost = "localhost";

	private String dbName = "gpadmin";

	private String dbUser = "gpadmin";

	private String dbPassword = "gpadmin";

	private int dbPort = 5432;

	@Override
	public Class<DataSource> getObjectType() {
		return DataSource.class;
	}

	@Override
	protected BasicDataSource createInstance() throws Exception {
		BasicDataSource ds = new BasicDataSource();
		ds.setDriverClassName("org.postgresql.Driver");
		if (StringUtils.hasText(dbUser)) {
			ds.setUsername(dbUser);
		}
		if (StringUtils.hasText(dbPassword)) {
			ds.setPassword(dbPassword);
		}
		ds.setUrl("jdbc:postgresql://" + dbHost + ":" + dbPort + "/" + dbName);
		return ds;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (controlFile != null) {
			if (StringUtils.hasText(controlFile.getHost())) {
				dbHost = controlFile.getHost();
			}
			if (StringUtils.hasText(controlFile.getDatabase())) {
				dbName = controlFile.getDatabase();
			}
			if (StringUtils.hasText(controlFile.getUser())) {
				dbUser = controlFile.getUser();
			}
			if (StringUtils.hasText(controlFile.getPassword())) {
				dbPassword = controlFile.getPassword();
			}
			if (controlFile.getPort() != null) {
				dbPort = controlFile.getPort();
			}
		}
		super.afterPropertiesSet();
	}

	@Override
	protected void destroyInstance(BasicDataSource instance) throws Exception {
		instance.close();
	}

	public void setControlFile(ControlFile controlFile) {
		this.controlFile = controlFile;
	}

	public void setDbHost(String dbHost) {
		this.dbHost = dbHost;
	}


	public void setDbName(String dbName) {
		this.dbName = dbName;
	}


	public void setDbUser(String dbUser) {
		this.dbUser = dbUser;
	}


	public void setDbPassword(String dbPassword) {
		this.dbPassword = dbPassword;
	}


	public void setDbPort(int dbPort) {
		this.dbPort = dbPort;
	}

}
