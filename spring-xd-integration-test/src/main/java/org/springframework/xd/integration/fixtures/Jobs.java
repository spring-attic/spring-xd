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

package org.springframework.xd.integration.fixtures;

import org.springframework.util.Assert;
import org.springframework.xd.integration.util.XdEnvironment;
import org.springframework.xd.test.fixtures.FileJdbcJob;


/**
 * A factory that provides fully instantiated job fixtures based on the environment selected at test startup.
 *
 * @author Glenn Renfro
 */
public class Jobs {

	private XdEnvironment environment;

	/**
	 * Initializes the instance with xdEnvironment
	 *
	 * @param environment
	 */
	public Jobs(XdEnvironment environment) {
		Assert.notNull(environment, "xdEnvironment must not be null");
		this.environment = environment;
	}

	/**
	 * Create an instance of the FileJdbc job with the default target dir, fileName, tableName and column names.
	 *
	 * @return instance of a FileJDBCJob Fixture.
	 */
	public FileJdbcJob fileJdbcJob() {
		return FileJdbcJob.withDefaults();
	}

}
