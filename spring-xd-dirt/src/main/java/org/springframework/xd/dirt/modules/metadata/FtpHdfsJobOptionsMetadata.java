/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.xd.dirt.modules.metadata;

import org.springframework.xd.module.options.mixins.BatchJobRestartableOptionMixin;
import org.springframework.xd.module.options.mixins.HadoopConfigurationMixin;
import org.springframework.xd.module.options.mixins.BatchJobSinglestepPartitionSupportOptionMixin;
import org.springframework.xd.module.options.spi.Mixin;
import org.springframework.xd.module.options.spi.ModuleOption;
import org.springframework.xd.module.options.types.Password;


/**
 * Describes the options for the ftphdfs job.
 *
 * @author Gary Russell
 * @author Thomas Risberg
 */
@Mixin({BatchJobRestartableOptionMixin.class, HadoopConfigurationMixin.class, BatchJobSinglestepPartitionSupportOptionMixin.class })
public class FtpHdfsJobOptionsMetadata {

	private String host;

	private int port = 21;

	private String username;

	private Password password;

	public String getHost() {
		return host;
	}

	@ModuleOption("the host name for the FTP server")
	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	@ModuleOption("the port for the FTP server")
	public void setPort(int port) {
		this.port = port;
	}

	public String getUsername() {
		return username;
	}

	@ModuleOption("the username for the FTP connection")
	public void setUsername(String username) {
		this.username = username;
	}

	public Password getPassword() {
		return password;
	}

	@ModuleOption("the password for the FTP connection")
	public void setPassword(Password password) {
		this.password = password;
	}

}
