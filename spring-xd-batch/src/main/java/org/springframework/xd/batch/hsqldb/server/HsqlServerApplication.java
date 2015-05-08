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

package org.springframework.xd.batch.hsqldb.server;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.batch.BatchDatabaseInitializer;
import org.springframework.boot.autoconfigure.batch.BatchProperties;
import org.springframework.boot.autoconfigure.integration.IntegrationAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.xd.batch.XdBatchDatabaseInitializer;


/**
 * Spring Application to operate life-cycle methods on {@link HSQLServerBean}.
 * 
 * @author Ilayaperumal Gopinathan
 * @author David Turanski
 */
@Configuration
@EnableAutoConfiguration(exclude = { BatchAutoConfiguration.class, IntegrationAutoConfiguration.class,
		 MongoAutoConfiguration.class, MongoDataAutoConfiguration.class})
@EnableConfigurationProperties(BatchProperties.class)
public class HsqlServerApplication {

	public final static String HSQLDBSERVER_PROFILE = "hsqldbServer";

	public static void main(String[] args) {
		new HsqlServerApplication().run(args);
	}

	public void run(String... args) {
		new SpringApplicationBuilder(HsqlDatasourceConfiguration.class)
				.profiles(HSQLDBSERVER_PROFILE)
				.child(HsqlServerApplication.class)
				.web(false)
				.showBanner(false)
				.run(args);
	}

	@Bean
	public BatchDatabaseInitializer batchDatabaseInitializer() {
		return new XdBatchDatabaseInitializer();
	}
}
