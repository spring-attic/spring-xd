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

package org.springframework.xd.test.fixtures;

import org.springframework.util.Assert;


/*
 * Copyright 2013-2014 the original author or authors.
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


/**
 * Test fixture that creates a Ftp Hdfs Job.
 *
 * @author Glenn Renfro
 * @author David Turanski
 */
public class FtpHdfsJob extends AbstractModuleFixture<FtpHdfsJob> {


	public static final String DEFAULT_HOST_NAME = "localhost";

	public static final String DEFAULT_PORT = "4444";


	private String hostName;

	private String port;

	/**
	 * 
	 * @return an FtpHdfsJob
	 */
	public static FtpHdfsJob withDefaults() {
		return new FtpHdfsJob(DEFAULT_HOST_NAME, DEFAULT_PORT);
	}

	/**
	 * 
	 * @param hostName the ftp server host name
	 * @param port the ftp server port
	 */
	public FtpHdfsJob(String hostName, String port) {
		Assert.hasText(hostName, "hostName must not be empty nor null");
		Assert.hasText(port, "port must not be empty nor null");
		this.hostName = hostName;
		this.port = port;
	}

	/**
	 * Renders the default DSL for this fixture.
	 */
	@Override
	public String toDSL() {
		StringBuilder dsl = new StringBuilder("ftphdfs");
		dsl.append(" --host=" + hostName);
		dsl.append(" --port=" + port);
		return dsl.toString();
	}
}
