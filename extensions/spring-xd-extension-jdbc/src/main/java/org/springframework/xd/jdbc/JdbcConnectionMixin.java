/*
 * Copyright 2013-2014 the original author or authors.
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
import org.springframework.xd.module.options.types.Password;

/**
 * Basic JDBC module/job options.
 *
 * @author Luke Taylor
 * @author Gunnar Hillert
 *
 */
public class JdbcConnectionMixin {

	protected String driverClassName;

	protected Password password;

	protected String url;

	protected String username;

	@ModuleOption("the JDBC driver to use")
	public void setDriverClassName(String driverClass) {
		this.driverClassName = driverClass;
	}

	@ModuleOption("the JDBC password")
	public void setPassword(Password password) {
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

	public Password getPassword() {
		return password;
	}

	public String getUrl() {
		return url;
	}

	public String getUsername() {
		return username;
	}

}
