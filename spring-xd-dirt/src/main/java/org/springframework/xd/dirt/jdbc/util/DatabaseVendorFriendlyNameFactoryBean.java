/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.xd.dirt.jdbc.util;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.support.JdbcUtils;


/**
 * A factory bean that returns a friendly string name from database metadata. Default names match those used by
 * JdbcMessageStore's init scripts, but can be overriden by {@link #setMappings(Map)}. Also, one can always provide an
 * {@link #setOverride(String) override} (presumably user-provided).
 * 
 * @author Eric Bottard
 */
public class DatabaseVendorFriendlyNameFactoryBean implements FactoryBean<String>, InitializingBean {

	private String friendlyName;

	private DataSource dataSource;

	private Map<String, String> vendorToFriendlyNames = new HashMap<String, String>();

	private String override;

	public DatabaseVendorFriendlyNameFactoryBean() {
		vendorToFriendlyNames.put("DB2", "db2");
		vendorToFriendlyNames.put("Apache Derby", "derby");
		vendorToFriendlyNames.put("H2", "h2");
		vendorToFriendlyNames.put("HSQL Database Engine", "hsqldb");
		vendorToFriendlyNames.put("MySQL", "mysql");
		vendorToFriendlyNames.put("Oracle", "oracle10g");
		vendorToFriendlyNames.put("PostgreSQL", "postgresql");
		vendorToFriendlyNames.put("Microsoft SQL Server", "sqlserver");
		vendorToFriendlyNames.put("Sybase", "sybase");

	}

	@Override
	public String getObject() throws Exception {
		return friendlyName;
	}

	@Override
	public Class<?> getObjectType() {
		return String.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (override != null) {
			friendlyName = override;
			return;
		}
		String vendorName = JdbcUtils.commonDatabaseName(JdbcUtils.extractDatabaseMetaData(dataSource,
				"getDatabaseProductName").toString());
		if (!vendorToFriendlyNames.containsKey(vendorName)) {
			throw new BeanInitializationException(String.format(
					"Detected database vendor name %s but no mapping found for it in %s", vendorName,
					vendorToFriendlyNames));
		}
		friendlyName = vendorToFriendlyNames.get(vendorName);
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public void setOverride(String override) {
		this.override = override;
	}

	public void setMappings(Map<String, String> mappings) {
		this.vendorToFriendlyNames = mappings;
	}

}
