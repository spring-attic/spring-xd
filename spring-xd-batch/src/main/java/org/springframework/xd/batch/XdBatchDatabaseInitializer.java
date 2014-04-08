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

package org.springframework.xd.batch;

import javax.sql.DataSource;

import org.springframework.batch.support.DatabaseType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.batch.BatchDatabaseInitializer;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

/**
 * A {@link BatchDatabaseInitializer} for XD batch datasource.
 * 
 * @author Ilayaperumal Gopinathan
 */

public class XdBatchDatabaseInitializer extends BatchDatabaseInitializer {

	private static final String REGISTRY_SCHEMA_LOCATION = "classpath:org/springframework/xd/batch/schema/registry-schema-@@platform@@.sql";

	@Autowired
	private DataSource dataSource;

	@Autowired
	private ResourceLoader resourceLoader;

	@Value("${spring.batch.initializer.enabled:true}")
	private boolean enabled = true;

	@Override
	protected void initialize() throws Exception {
		super.initialize();
		if (enabled) {
			String platform = DatabaseType.fromMetaData(dataSource).toString().toLowerCase();
			if ("hsql".equals(platform))
				platform = "hsqldb";
			if ("postgres".equals(platform))
				platform = "postgresql";
			ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
			populator.addScript(resourceLoader.getResource(REGISTRY_SCHEMA_LOCATION.replace("@@platform@@", platform)));
			populator.setContinueOnError(true);
			DatabasePopulatorUtils.execute(populator, dataSource);
		}
	}
}
