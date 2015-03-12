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

package org.springframework.xd.gpload;

import org.hibernate.validator.constraints.NotBlank;
import org.springframework.util.StringUtils;
import org.springframework.xd.module.options.spi.ModuleOption;

import javax.validation.constraints.AssertTrue;

/**
 * Module options for Gpload batch job module.
 *
 * @author Thomas Risberg
 */
public class GploadOptionsMetadata {

	private String gploadHome;

	private String controlFile;

	private String options;

	private String database;

	private String host;

	private Integer port;

	private String username;

	private String password;

	private String passwordFile;


	@NotBlank
	public String getGploadHome() {
		return gploadHome;
	}

	@ModuleOption("the gpload home location")
	public void setGploadHome(String gploadHome) {
		this.gploadHome = gploadHome;
	}

	@NotBlank
	public String getControlFile() {
		return controlFile;
	}

	@ModuleOption("path to the gpload control file")
	public void setControlFile(String controlFile) {
		this.controlFile = controlFile;
	}

	public String getOptions() {
		return options;
	}

	@ModuleOption("the gpload options to use")
	public void setOptions(String options) {
		this.options = options;
	}

	public String getDatabase() {
		return database;
	}

	@ModuleOption("the name of the database to load into")
	public void setDatabase(String database) {
		this.database = database;
	}

	public String getHost() {
		return host;
	}

	@ModuleOption("the host name for the Greenplum master database server")
	public void setHost(String host) {
		this.host = host;
	}

	public Integer getPort() {
		return port;
	}

	@ModuleOption("the port for the Greenplum master database server")
	public void setPort(Integer port) {
		this.port = port;
	}

	public String getUsername() {
		return username;
	}

	@ModuleOption("the username to connect as")
	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	@ModuleOption("the password to use when connecting")
	public void setPassword(String password) {
		this.password = password;
	}

	public String getPasswordFile() {
		return passwordFile;
	}

	@ModuleOption("the location of the password file")
	public void setPasswordFile(String passwordFile) {
		this.passwordFile = passwordFile;
	}

	@AssertTrue(message = "Use 'passwordFile' OR 'password' to specify the password")
	boolean isRequiredPasswordOrPasswordFile() {
		if (StringUtils.hasText(password)) {
			return true;
		}
		else if (StringUtils.hasText(passwordFile)) {
			return true;
		}
		else {
			return false;
		}
	}

	@AssertTrue(message = "Use 'passwordFile' OR 'password' to specify the password, you can't use both")
	boolean isEitherPasswordOrPasswordFile() {
		if (StringUtils.hasText(password)) {
			return !StringUtils.hasText(passwordFile);
		}
		else {
			return true;
		}
	}
}
