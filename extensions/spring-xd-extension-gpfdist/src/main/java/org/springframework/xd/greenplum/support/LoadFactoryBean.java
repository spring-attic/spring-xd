/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.xd.greenplum.support;

import java.io.IOException;

import javax.sql.DataSource;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.Assert;

/**
 * FactoryBean for easy creation and configuration of {@link GreenplumLoad}
 * instances.
 *
 * @author Janne Valkealahti
 *
 */
public class LoadFactoryBean implements FactoryBean<GreenplumLoad>, InitializingBean, DisposableBean {

	private DataSource dataSource;

	private LoadConfiguration loadConfiguration;

	private JdbcTemplate jdbcTemplate;

	@Override
	public GreenplumLoad getObject() throws Exception {
		return new DefaultGreenplumLoad(loadConfiguration, new DefaultLoadService(jdbcTemplate));
	}

	@Override
	public Class<GreenplumLoad> getObjectType() {
		return GreenplumLoad.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public void afterPropertiesSet() throws IOException {
		Assert.notNull(dataSource, "DataSource must not be null.");
		Assert.notNull(loadConfiguration, "Load configuration must not be null.");
		jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Override
	public void destroy() {
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public void setLoadConfiguration(LoadConfiguration LoadConfiguration) {
		this.loadConfiguration = LoadConfiguration;
	}

}
