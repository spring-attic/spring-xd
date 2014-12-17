/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.xd.syslog;


import javax.validation.constraints.Pattern;

import org.springframework.xd.module.options.spi.ModuleOption;
import org.springframework.xd.module.options.spi.ProfileNamesProvider;

/**
 * Captures options for the {@code syslog-udp} source module.
 *
 * @author Gary Russell
 * @since 1.1
 *
 */
public class SyslogSourceOptionsMetadata implements ProfileNamesProvider{

	private String rfc = "3164";

	private int port = 5140;

	@Pattern(regexp = "(3164|5424)")
	public String getRfc() {
		return rfc;
	}

	@ModuleOption("the format of the syslog")
	public void setRfc(String rfc) {
		this.rfc = rfc;
	}

	public int getPort() {
		return port;
	}

	@ModuleOption("the port on which to listen")
	public void setPort(int port) {
		this.port = port;
	}

	@Override
	public String[] profilesToActivate() {
		return new String[] { "rfc" + rfc };
	}

}
